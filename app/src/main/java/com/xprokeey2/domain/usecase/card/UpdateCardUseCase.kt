package com.xprokeey2.domain.usecase.card

import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardChanges
import com.xprokeey2.domain.model.CardUpdatePayload
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Edit card, the web way (spec §8.4): only changed fields are sent. A new number goes with its
 * last4 and brand; number and CVC are encrypted with the vault key first.
 */
class UpdateCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(cardId: Long, changes: CardChanges): Resource<Card> {
        val needsVault = changes.number != null || changes.cvc != null
        val vaultKey = vaultSession.vaultKey
        if (needsVault && vaultKey == null) return Resource.Error(DataError.VaultLocked)

        val number = changes.number?.filter(Char::isDigit)
        val payload = CardUpdatePayload(
            label = changes.label?.trim(),
            holderName = changes.holderName?.trim(),
            category = changes.category,
            encryptedNumber = number?.let { vaultCrypto.encryptWithVaultKey(it, vaultKey!!) },
            encryptedCvc = changes.cvc?.let { vaultCrypto.encryptWithVaultKey(it, vaultKey!!) },
            last4 = number?.takeLast(4),
            brand = if (number != null) changes.brand else null,
            expiry = changes.expiry,
            bankName = changes.bankName?.trim(),
            notes = changes.notes?.trim(),
        )
        return cardRepository.updateCard(cardId, payload)
    }
}
