package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.model.VaultItemDetails
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Loads one item and decrypts its password with the in-memory vault key. */
class GetVaultItemDetailsUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(id: Long): Resource<VaultItemDetails> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        return when (val result = vaultRepository.getItem(id)) {
            is Resource.Success -> {
                val encrypted = result.data.encryptedPassword
                val password = if (encrypted.isBlank()) "" else vaultCrypto.decryptWithVaultKey(encrypted, vaultKey)
                Resource.Success(VaultItemDetails(result.data.item, password))
            }
            is Resource.Error -> result
        }
    }
}
