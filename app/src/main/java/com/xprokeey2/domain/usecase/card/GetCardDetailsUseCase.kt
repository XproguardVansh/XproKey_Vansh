package com.xprokeey2.domain.usecase.card

import com.xprokeey2.domain.model.CardDetails
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Loads one card and decrypts its number and CVC with the in-memory vault key. */
class GetCardDetailsUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(id: Long): Resource<CardDetails> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        return when (val result = cardRepository.getCard(id)) {
            is Resource.Success -> Resource.Success(
                CardDetails(
                    card = result.data.card,
                    number = open(result.data.encryptedNumber, vaultKey),
                    cvc = open(result.data.encryptedCvc, vaultKey),
                )
            )
            is Resource.Error -> result
        }
    }

    /** "" when nothing was stored, null when the value isn't ciphertext under this vault key. */
    private suspend fun open(payload: String, vaultKey: String): String? =
        if (payload.isBlank()) "" else vaultCrypto.decryptWithVaultKey(payload, vaultKey)
}
