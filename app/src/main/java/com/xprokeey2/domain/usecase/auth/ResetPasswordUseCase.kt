package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Step 2 of "forgot password", done the same way as the web app: the server can't decrypt the
 * vault, so the recovery key unlocks it here and the same vault key is re-locked with the new
 * password under the account's existing salt. Saved items stay readable and the recovery key
 * keeps working.
 */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val vaultCrypto: VaultCrypto,
) {
    suspend operator fun invoke(
        email: String,
        otp: String,
        recoveryKey: String,
        newPassword: String,
        confirmPassword: String,
        masterSalt: String,
        encryptedVaultKeyRecovery: String,
    ): Resource<String> {
        val encryptedVaultKey = if (encryptedVaultKeyRecovery.isBlank() || masterSalt.isBlank()) {
            null // Account has no vault (created before vault keys existed): only the password changes.
        } else {
            val vaultKey = vaultCrypto.recoverVaultKey(
                // Tolerate spaces/line breaks from copying the key off the dialog or the CSV.
                recoveryKey = recoveryKey.filterNot(Char::isWhitespace),
                encryptedVaultKeyRecovery = encryptedVaultKeyRecovery,
            ) ?: return Resource.Error(DataError.InvalidRecoveryKey)
            vaultCrypto.lockVaultKey(vaultKey = vaultKey, masterPassword = newPassword, masterSalt = masterSalt)
        }

        return authRepository.resetPassword(
            email = email.trim(),
            otp = otp.trim(),
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            encryptedVaultKey = encryptedVaultKey,
        )
    }
}
