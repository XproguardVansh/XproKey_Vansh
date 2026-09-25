package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.RecoveryKeyRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Creates the vault keys on the device (zero-knowledge), registers the account with only
 * the encrypted keys, then keeps the recovery key locally until the email is verified.
 */
class SignupUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val recoveryKeyRepository: RecoveryKeyRepository,
    private val vaultCrypto: VaultCrypto,
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Resource<SignupResult> {
        val vault = vaultCrypto.createVault(masterPassword = password)

        val result = authRepository.signup(
            name = name.trim(),
            email = email.trim(),
            password = password,
            confirmPassword = confirmPassword,
            vaultKeys = vault.encryptedKeys,
        )
        if (result is Resource.Success) {
            recoveryKeyRepository.savePendingKey(result.data.user.email, vault.recoveryKey)
        }
        return result
    }
}
