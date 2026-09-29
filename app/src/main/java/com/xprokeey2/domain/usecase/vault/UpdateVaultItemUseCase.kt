package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.model.VaultItemChanges
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import com.xprokeey2.domain.model.isPasswordWeak
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.repository.WeakVaultItemRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Edit: sends only changed fields; a new password is encrypted with the vault key first, and once
 * saved the item is marked weak or not again (`markVaultItemWeakness`).
 */
class UpdateVaultItemUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val weakItems: WeakVaultItemRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    /** Returns the server message. */
    suspend operator fun invoke(id: Long, changes: VaultItemChanges): Resource<String> {
        val encryptedPassword = changes.password?.let { password ->
            val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
            vaultCrypto.encryptWithVaultKey(password, vaultKey)
        }
        val payload = VaultItemUpdatePayload(
            title = changes.title?.trim(),
            username = changes.username?.trim(),
            url = changes.url?.trim(),
            encryptedPassword = encryptedPassword,
            category = changes.category,
            notes = changes.notes?.trim(),
            isFavorite = changes.isFavorite,
        )
        val result = vaultRepository.updateItem(id, payload)
        if (result is Resource.Success && changes.password != null) {
            weakItems.markWeakness(id, isPasswordWeak(changes.password))
        }
        return result
    }
}
