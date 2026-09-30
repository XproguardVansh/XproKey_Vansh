package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import javax.inject.Inject

enum class UnlockVaultResult {
    UNLOCKED,
    WRONG_PASSWORD,

    /** No saved encrypted vault key or salt on this device: the user has to log in again. */
    SESSION_DATA_MISSING,
}

/** The Lock screen, like the web's: the master password opens the vault key saved at login. */
class UnlockVaultUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(masterPassword: String): UnlockVaultResult {
        val encryptedVaultKey = sessionRepository.getEncryptedVaultKey()?.takeIf { it.isNotBlank() }
        val masterSalt = sessionRepository.getMasterSalt()?.takeIf { it.isNotBlank() }
        if (encryptedVaultKey == null || masterSalt == null) return UnlockVaultResult.SESSION_DATA_MISSING

        val vaultKey = vaultCrypto.unlockVaultKey(
            masterPassword = masterPassword,
            masterSalt = masterSalt,
            encryptedVaultKey = encryptedVaultKey,
        ) ?: return UnlockVaultResult.WRONG_PASSWORD

        vaultSession.unlock(vaultKey)
        return UnlockVaultResult.UNLOCKED
    }
}
