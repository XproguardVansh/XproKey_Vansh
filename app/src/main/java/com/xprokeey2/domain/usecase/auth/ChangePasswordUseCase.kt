package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Settings > Change password, like the web page: the signed-in vault key (in memory) is locked
 * again with the new password under the account's saved master salt, and /reset-password stores
 * it with the new password after checking the emailed OTP. Returns the server message.
 */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String,
    ): Resource<String> {
        // Web: "Vault locked or session expired. Please login again."
        val vaultKey = vaultSession.vaultKey ?: return Resource.Error(DataError.VaultLocked)
        val masterSalt = sessionRepository.getMasterSalt()?.takeIf { it.isNotBlank() }
            ?: return Resource.Error(DataError.VaultLocked)

        val encryptedVaultKey = vaultCrypto.lockVaultKey(vaultKey = vaultKey, masterPassword = newPassword, masterSalt = masterSalt)
        val result = authRepository.resetPassword(
            email = email.trim(),
            otp = otp.trim(),
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            encryptedVaultKey = encryptedVaultKey,
        )
        if (result is Resource.Success) sessionRepository.saveEncryptedVaultKey(encryptedVaultKey)
        return result
    }
}
