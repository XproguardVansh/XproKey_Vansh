package com.xprokeey2.domain.usecase.card

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardDraft
import com.xprokeey2.domain.model.CardExpiry
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.model.StoredCard
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardUseCasesTest {

    private val crypto = VaultCryptoImpl()

    private class FakeVaultSession(override var vaultKey: String?) : VaultSession {
        override fun unlock(vaultKey: String) {
            this.vaultKey = vaultKey
        }

        override fun lock() {
            vaultKey = null
        }
    }

    private class FakeCardRepository(private val stored: StoredCard? = null) : CardRepository {
        var created: CardPayload? = null
        var updated: Pair<Long, CardPayload>? = null

        override suspend fun getCards(): Resource<List<Card>> = Resource.Success(listOfNotNull(stored?.card))
        override suspend fun getCard(id: Long): Resource<StoredCard> = Resource.Success(stored!!)
        override suspend fun createCard(payload: CardPayload): Resource<Card> {
            created = payload
            return Resource.Success(CARD)
        }

        override suspend fun updateCard(id: Long, payload: CardPayload): Resource<Card> {
            updated = id to payload
            return Resource.Success(CARD)
        }

        override suspend fun deleteCard(id: Long): Resource<String> = Resource.Success("Card deleted successfully")
    }

    private val draft = CardDraft(
        label = " Personal ",
        holderName = "Vansh Goel",
        number = "65228143420114",
        cvc = "123",
        expiry = CardExpiry(month = 7, year = 2028),
        category = CardCategory.DEBIT,
        bankName = "BOB",
        notes = "",
    )

    @Test
    fun savingEncryptsNumberAndCvcWithTheVaultKey() = runBlocking {
        val repository = FakeCardRepository()
        val result = SaveCardUseCase(repository, crypto, FakeVaultSession(VAULT_KEY))(draft)

        assertTrue(result is Resource.Success)
        val payload = repository.created!!
        assertNotEquals(draft.number, payload.encryptedNumber)
        assertEquals(draft.number, crypto.decryptWithVaultKey(payload.encryptedNumber, VAULT_KEY))
        assertEquals("123", crypto.decryptWithVaultKey(payload.encryptedCvc, VAULT_KEY))
        assertEquals("0114", payload.last4)
        assertEquals(CardBrand.RUPAY, payload.brand)
        assertEquals("Personal", payload.label)
        assertEquals(CardExpiry(7, 2028), payload.expiry)
    }

    @Test
    fun editingReplacesTheSameCard() = runBlocking {
        val repository = FakeCardRepository()
        SaveCardUseCase(repository, crypto, FakeVaultSession(VAULT_KEY))(draft, cardId = 267)
        assertEquals(267L, repository.updated?.first)
        assertNull(repository.created)
    }

    @Test
    fun lockedVaultCantSaveCards() = runBlocking {
        val repository = FakeCardRepository()
        val result = SaveCardUseCase(repository, crypto, FakeVaultSession(null))(draft)
        assertEquals(Resource.Error(DataError.VaultLocked), result)
        assertNull(repository.created)
    }

    @Test
    fun detailsDecryptWebCiphertextAndFlagPlainValues() = runBlocking {
        // Number from the web app (vault-key ciphertext); CVC saved in plain text from Postman.
        val stored = StoredCard(
            card = CARD,
            encryptedNumber = "tqh+LtR1Igu2hYHOqJuEhGjWg7FsBgSPesDkFnLkxlSn3c6yoKeU3g/1BPE=",
            encryptedCvc = "1234",
        )
        val result = GetCardDetailsUseCase(FakeCardRepository(stored), crypto, FakeVaultSession(VAULT_KEY))(270)

        val details = (result as Resource.Success).data
        assertEquals("4111111111111111", details.number)
        assertNull(details.cvc)
    }

    private companion object {
        const val VAULT_KEY = "TQkzi8jV90LIkYuRaWsDS+2eg5H290vBdeLpW2GgCv8="
        val CARD = Card(270, "Visa Test", "Shreyash Jadhav", "credit", CardBrand.VISA, "1111", "", "", 12, 2030)
    }
}
