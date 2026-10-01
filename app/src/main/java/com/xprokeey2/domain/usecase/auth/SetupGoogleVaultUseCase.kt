package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.model.GoogleVaultSetup
import com.xprokeey2.domain.repository.GoogleAuthRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * "Create Your Master Password" for a new Google account, like the web: the vault key, salt and
 * recovery key are made on the device (zero-knowledge), the encrypted keys are sent with the setup
 * token, and the returned session is saved with the new vault already open.
 */
class SetupGoogleVaultUseCase @Inject constructor(
    private val repository: GoogleAuthRepository,
    private val vaultCrypto: VaultCrypto,
    private val vaultSession: VaultSession,
) {
    /** [name] and [email] come from POST /auth/google, in case the setup answer leaves them out. */
    suspend operator fun invoke(
        setupToken: String,
        password: String,
        confirmPassword: String,
        name: String,
        email: String,
    ): Resource<GoogleVaultSetup> {
        val vault = vaultCrypto.createVault(masterPassword = password)

        val returned = when (
            val result = repository.setupVaultPassword(
                setupToken = setupToken,
                password = password,
                confirmPassword = confirmPassword,
                vaultKeys = vault.encryptedKeys,
            )
        ) {
            is Resource.Error -> return result
            is Resource.Success -> result.data
        }

        val session = returned?.withFallbacks(vault.encryptedKeys, name = name, email = email)
        if (session != null) {
            repository.saveSession(session)
            vaultSession.unlock(vault.vaultKey)
        }
        return Resource.Success(
            GoogleVaultSetup(
                recoveryKey = vault.recoveryKey,
                isSignedIn = session != null,
                needsAccountSetup = session?.needsAccountSetup ?: true,
            )
        )
    }

    /** The web keeps the server's copies when it sends them; otherwise the keys just created are used. */
    private fun GoogleSession.withFallbacks(keys: EncryptedVaultKeys, name: String, email: String) = copy(
        masterSalt = masterSalt.ifBlank { keys.masterSalt },
        encryptedVaultKey = encryptedVaultKey.ifBlank { keys.encryptedVaultKey },
        user = user.copy(
            name = user.name.ifBlank { name },
            email = user.email.ifBlank { email },
        ),
    )
}
