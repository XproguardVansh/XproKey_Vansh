package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.StoredVaultItem
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemChanges
import com.xprokeey2.domain.model.VaultItemDraft
import com.xprokeey2.domain.model.VaultItemPayload
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.repository.WeakVaultItemRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class VaultUseCasesTest {

    private val crypto = VaultCryptoImpl()

    private class FakeVaultSession(override var vaultKey: String?) : VaultSession {
        override fun unlock(vaultKey: String) {
            this.vaultKey = vaultKey
        }

        override fun lock() {
            vaultKey = null
        }
    }

    private class FakeVaultRepository : VaultRepository {
        var created: VaultItemPayload? = null
        var updated: Pair<Long, VaultItemUpdatePayload>? = null
        val deleted = mutableListOf<Long>()
        var failDeleteOf: Long? = null
        var failUpdate = false

        override suspend fun getItems(): Resource<List<VaultItem>> = Resource.Success(emptyList())
        override suspend fun getItem(id: Long): Resource<StoredVaultItem> = Resource.Success(StoredVaultItem(item(id), ""))

        override suspend fun createItem(payload: VaultItemPayload): Resource<VaultItem> {
            created = payload
            return Resource.Success(item(781))
        }

        override suspend fun updateItem(id: Long, payload: VaultItemUpdatePayload): Resource<String> {
            if (failUpdate) return Resource.Error(DataError.Server(404, "Vault item not found"))
            updated = id to payload
            return Resource.Success("Vault item updated successfully")
        }

        override suspend fun deleteItem(id: Long): Resource<String> {
            if (id == failDeleteOf) return Resource.Error(DataError.Server(404, "Vault item not found"))
            deleted += id
            return Resource.Success("Vault item deleted successfully")
        }

        override suspend fun getCategories(): Resource<List<String>> = Resource.Success(emptyList())
        override suspend fun createCategory(name: String): Resource<String> = Resource.Success(name)
    }

    /** The web's localStorage list, in memory. */
    private class FakeWeakItems(vararg initial: Long) : WeakVaultItemRepository {
        val ids = initial.toMutableSet()

        override suspend fun getWeakItemIds(): Set<Long> = ids.toSet()

        override suspend fun markWeakness(itemId: Long, isWeak: Boolean) {
            if (isWeak) ids += itemId else ids -= itemId
        }

        override suspend fun remove(itemId: Long) {
            ids -= itemId
        }
    }

    private fun draft(password: String) =
        VaultItemDraft(" Github ", "Vanshgoel2610", "https://github.com", password, "Personal", "test account", true)

    @Test
    fun addingEncryptsOnlyThePassword() = runBlocking {
        val repository = FakeVaultRepository()
        val draft = draft("cVkd,FO)w[n09vR8")

        AddVaultItemUseCase(repository, FakeWeakItems(), crypto, FakeVaultSession(VAULT_KEY))(draft)

        val payload = repository.created!!
        assertNotEquals(draft.password, payload.encryptedPassword)
        assertEquals(draft.password, crypto.decryptWithVaultKey(payload.encryptedPassword, VAULT_KEY))
        assertEquals("Github", payload.title)
        assertEquals("test account", payload.notes) // notes stay plain text, like the web
        assertTrue(payload.isFavorite)
    }

    @Test
    fun addingMarksTheNewItemWeakOrNot() = runBlocking {
        val weakItems = FakeWeakItems()
        AddVaultItemUseCase(FakeVaultRepository(), weakItems, crypto, FakeVaultSession(VAULT_KEY))(draft("walldfs"))
        assertEquals(setOf(781L), weakItems.ids)

        val strongItems = FakeWeakItems()
        AddVaultItemUseCase(FakeVaultRepository(), strongItems, crypto, FakeVaultSession(VAULT_KEY))(draft("cVkd,FO)w[n09vR8"))
        assertEquals(emptySet<Long>(), strongItems.ids)
    }

    @Test
    fun lockedVaultCantAdd() = runBlocking {
        val weakItems = FakeWeakItems()
        val result = AddVaultItemUseCase(FakeVaultRepository(), weakItems, crypto, FakeVaultSession(null))(draft("d"))
        assertEquals(Resource.Error(DataError.VaultLocked), result)
        assertEquals(emptySet<Long>(), weakItems.ids)
    }

    @Test
    fun editSendsOnlyChangedFieldsAndEncryptsANewPassword() = runBlocking {
        val repository = FakeVaultRepository()
        UpdateVaultItemUseCase(repository, FakeWeakItems(), crypto, FakeVaultSession(VAULT_KEY))(
            781,
            VaultItemChanges(title = "jjsjdj", password = "N3w!pass"),
        )

        val (id, payload) = repository.updated!!
        assertEquals(781L, id)
        assertEquals("jjsjdj", payload.title)
        assertEquals("N3w!pass", crypto.decryptWithVaultKey(payload.encryptedPassword!!, VAULT_KEY))
        assertNull(payload.username)
        assertNull(payload.notes)
    }

    @Test
    fun newPasswordMarksTheItemAgain() = runBlocking {
        // AppLock edited to "walldfs" becomes weak; a strong password takes it off the list.
        val weakItems = FakeWeakItems()
        val update = UpdateVaultItemUseCase(FakeVaultRepository(), weakItems, crypto, FakeVaultSession(VAULT_KEY))

        update(2, VaultItemChanges(password = "walldfs"))
        assertEquals(setOf(2L), weakItems.ids)

        update(2, VaultItemChanges(password = "cVkd,FO)w[n09vR8"))
        assertEquals(emptySet<Long>(), weakItems.ids)
    }

    @Test
    fun editWithoutPasswordLeavesTheWeakListAlone() = runBlocking {
        val weakItems = FakeWeakItems(2)
        val result = UpdateVaultItemUseCase(FakeVaultRepository(), weakItems, crypto, FakeVaultSession(null))(
            2,
            VaultItemChanges(isFavorite = true),
        )
        assertTrue(result is Resource.Success) // no password sent, so no vault key needed
        assertEquals(setOf(2L), weakItems.ids)
    }

    @Test
    fun failedEditDoesNotMark() = runBlocking {
        val weakItems = FakeWeakItems()
        val repository = FakeVaultRepository().apply { failUpdate = true }
        UpdateVaultItemUseCase(repository, weakItems, crypto, FakeVaultSession(VAULT_KEY))(2, VaultItemChanges(password = "walldfs"))
        assertEquals(emptySet<Long>(), weakItems.ids)
    }

    @Test
    fun deletesOneByOneAndDropsThemFromTheWeakList() = runBlocking {
        val weakItems = FakeWeakItems(1, 3, 4)
        val repository = FakeVaultRepository().apply { failDeleteOf = 3 }
        val result = DeleteVaultItemsUseCase(repository, weakItems)(listOf(1L, 2L, 3L, 4L))

        assertTrue(result is Resource.Error)
        assertEquals(listOf(1L, 2L), repository.deleted)
        assertEquals(setOf(3L, 4L), weakItems.ids) // 3 failed and 4 wasn't reached: both stay
    }

    @Test
    fun generatorFollowsTheWebRules() {
        val generate = GeneratePasswordUseCase()
        repeat(50) {
            val password = generate()
            assertEquals(16, password.length)
            assertTrue(password.any { it in 'A'..'Z' })
            assertTrue(password.any { it in 'a'..'z' })
            assertTrue(password.any { it in '0'..'9' })
            assertTrue(password.any { it in "!@#$%^&*()_+-=[]{}|;:,.<>?" })
        }
        val noAmbiguous = generate(PasswordGeneratorOptions(length = 40, avoidAmbiguous = true))
        assertTrue(noAmbiguous.none { it in "IOlo01" })
        assertEquals("", generate(PasswordGeneratorOptions(uppercase = false, lowercase = false, numbers = false, symbols = false)))
    }

    private companion object {
        const val VAULT_KEY = "TQkzi8jV90LIkYuRaWsDS+2eg5H290vBdeLpW2GgCv8="

        fun item(id: Long) = VaultItem(id, "Item $id", "user$id", "https://site$id.com", "", "Work", false, null, Instant.EPOCH)
    }
}
