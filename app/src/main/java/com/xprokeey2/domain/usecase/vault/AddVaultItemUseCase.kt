package com.xprokeey2.domain.usecase.vault

import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemDraft
import com.xprokeey2.domain.model.VaultItemPayload
import com.xprokeey2.domain.model.isPasswordWeak
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.repository.WeakVaultItemRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Saves a new password. Like the web app, only the password is encrypted (with the vault key),
 * and the new item is marked weak or not (`markVaultItemWeakness`).
 */
class AddVaultItemUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val weakItems: WeakVaultItemRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(draft: VaultItemDraft): Resource<VaultItem> {
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        val payload = VaultItemPayload(
            title = draft.title.trim(),
            username = draft.username.trim(),
            url = draft.url.trim(),
            encryptedPassword = vaultCrypto.encryptWithVaultKey(draft.password, vaultKey),
            category = draft.category,
            notes = draft.notes.trim(),
            isFavorite = draft.isFavorite,
        )
        val result = vaultRepository.createItem(payload)
        if (result is Resource.Success) weakItems.markWeakness(result.data.id, isPasswordWeak(draft.password))
        return result
    }
}
