package com.xprokeey2.domain.usecase.card

import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardDraft
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Creates a card, or replaces every field of card [cardId]. Like the web app, the number and CVC
 * are encrypted with the vault key before they leave the device; last4 and brand stay readable so
 * lists can be shown without decrypting.
 */
class SaveCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(draft: CardDraft, cardId: Long? = null): Resource<Card> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        val number = draft.number.filter(Char::isDigit)
        val payload = CardPayload(
            label = draft.label.trim(),
            holderName = draft.holderName.trim(),
            category = draft.category,
            encryptedNumber = vaultCrypto.encryptWithVaultKey(number, vaultKey),
            encryptedCvc = vaultCrypto.encryptWithVaultKey(draft.cvc, vaultKey),
            last4 = number.takeLast(4),
            brand = CardBrand.detect(number),
            expiry = draft.expiry,
            bankName = draft.bankName.trim(),
            notes = draft.notes.trim(),
        )
        return if (cardId == null) {
            cardRepository.createCard(payload)
        } else {
            cardRepository.updateCard(cardId, payload)
        }
    }
}
