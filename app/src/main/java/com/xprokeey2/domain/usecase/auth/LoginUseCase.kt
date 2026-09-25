package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Signs in, then unlocks the vault key with the master password and keeps it in memory. */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(email: String, password: String): Resource<LoginResult> {
        val result = authRepository.login(email = email.trim(), password = password)
        if (result !is Resource.Success) return result

        // Nothing to unlock: the account has no vault yet. Stay signed in, vault stays locked.
        if (!result.data.hasVaultKeys) {
            vaultSession.lock()
            return result
        }

        val vaultKey = vaultCrypto.unlockVaultKey(
            masterPassword = password,
            masterSalt = result.data.masterSalt,
            encryptedVaultKey = result.data.encryptedVaultKey,
        )
        if (vaultKey == null) {
            // Don't leave a half-signed-in session behind if the vault can't be opened.
            authRepository.clearSession()
            return Resource.Error(DataError.VaultUnlockFailed)
        }

        vaultSession.unlock(vaultKey)
        return result
    }
}
