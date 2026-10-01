package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.repository.GoogleAuthRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import javax.inject.Inject

/**
 * "Enter Master Password" after a returning user signs in with Google: the master password opens the
 * vault key, and only then is the session saved, like the web. False for a wrong password.
 */
class UnlockGoogleVaultUseCase @Inject constructor(
    private val repository: GoogleAuthRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(session: GoogleSession, masterPassword: String): Boolean {
        val vaultKey = vaultCrypto.unlockVaultKey(
            masterPassword = masterPassword,
            masterSalt = session.masterSalt,
            encryptedVaultKey = session.encryptedVaultKey,
        ) ?: return false

        repository.saveSession(session)
        vaultSession.unlock(vaultKey)
        return true
    }
}
