package com.xprokeey2.domain.usecase.card

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardChanges
import com.xprokeey2.domain.model.CardDraft
import com.xprokeey2.domain.model.CardExpiry
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.model.CardUpdatePayload
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
        var updated: Pair<Long, CardUpdatePayload>? = null

        override suspend fun getCards(): Resource<List<Card>> = Resource.Success(listOfNotNull(stored?.card))
        override suspend fun getCard(id: Long): Resource<StoredCard> = Resource.Success(stored!!)
        override suspend fun createCard(payload: CardPayload): Resource<Card> {
            created = payload
            return Resource.Success(CARD)
        }

        override suspend fun updateCard(id: Long, payload: CardUpdatePayload): Resource<Card> {
            updated = id to payload
            return Resource.Success(CARD)
        }

        override suspend fun deleteCard(id: Long): Resource<String> = Resource.Success("Card deleted successfully")
    }

    private val draft = CardDraft(
        label = " Personal ",
        holderName = "Vansh Goel",
        number = "4640464646484465",
        cvc = "578",
        expiry = CardExpiry(month = 7, year = 2030),
        category = CardCategory.CORPORATE,
        brand = CardBrand.VISA,
        bankName = "Bo",
        notes = "",
    )

    @Test
    fun addingEncryptsNumberAndCvcWithTheVaultKey() = runBlocking {
        val repository = FakeCardRepository()
        val result = AddCardUseCase(repository, crypto, FakeVaultSession(VAULT_KEY))(draft)

        assertTrue(result is Resource.Success)
        val payload = repository.created!!
        assertNotEquals(draft.number, payload.encryptedNumber)
        assertEquals(draft.number, crypto.decryptWithVaultKey(payload.encryptedNumber, VAULT_KEY))
        assertEquals("578", crypto.decryptWithVaultKey(payload.encryptedCvc, VAULT_KEY))
        assertEquals("4465", payload.last4)
        assertEquals(CardBrand.VISA, payload.brand)
        assertEquals("Personal", payload.label)
        // Corporate cards are saved as "credit": the server only accepts credit / debit.
        assertEquals("credit", payload.category.cardType)
    }

    @Test
    fun lockedVaultCantAddCards() = runBlocking {
        val repository = FakeCardRepository()
        val result = AddCardUseCase(repository, crypto, FakeVaultSession(null))(draft)
        assertEquals(Resource.Error(DataError.VaultLocked), result)
        assertNull(repository.created)
    }

    @Test
    fun editSendsOnlyChangedFields() = runBlocking {
        val repository = FakeCardRepository()
        UpdateCardUseCase(repository, crypto, FakeVaultSession(VAULT_KEY))(
            cardId = 267,
            changes = CardChanges(notes = " New limit "),
        )

        val (id, payload) = repository.updated!!
        assertEquals(267L, id)
        assertEquals(CardUpdatePayload(notes = "New limit"), payload)
    }

    @Test
    fun editedNumberIsEncryptedAndSentWithLast4AndBrand() = runBlocking {
        val repository = FakeCardRepository()
        UpdateCardUseCase(repository, crypto, FakeVaultSession(VAULT_KEY))(
            cardId = 267,
            changes = CardChanges(number = "6521000000000007", brand = CardBrand.RUPAY),
        )

        val payload = repository.updated!!.second
        assertEquals("6521000000000007", crypto.decryptWithVaultKey(payload.encryptedNumber!!, VAULT_KEY))
        assertEquals("0007", payload.last4)
        assertEquals(CardBrand.RUPAY, payload.brand)
        assertNull(payload.encryptedCvc)
    }

    @Test
    fun editWithoutSecretsDoesNotNeedTheVault() = runBlocking {
        val repository = FakeCardRepository()
        val result = UpdateCardUseCase(repository, crypto, FakeVaultSession(null))(267, CardChanges(label = "Travel"))
        assertTrue(result is Resource.Success)

        val locked = UpdateCardUseCase(repository, crypto, FakeVaultSession(null))(267, CardChanges(cvc = "123"))
        assertEquals(Resource.Error(DataError.VaultLocked), locked)
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
