package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetPasswordUseCaseTest {

    private class RecordingAuthRepository : AuthRepository {
        var called = false
        var sentPassword: String? = null
        var sentEncryptedVaultKey: String? = null

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmPassword: String,
            encryptedVaultKey: String?,
        ): Resource<String> {
            called = true
            sentPassword = newPassword
            sentEncryptedVaultKey = encryptedVaultKey
            return Resource.Success("Password reset successfully")
        }

        override suspend fun signup(
            name: String,
            email: String,
            password: String,
            confirmPassword: String,
            vaultKeys: EncryptedVaultKeys,
        ): Resource<SignupResult> = error("unused")

        override suspend fun login(email: String, password: String): Resource<LoginResult> = error("unused")
        override suspend fun verifyAccount(email: String, otp: String): Resource<String> = error("unused")
        override suspend fun resendSignupOtp(email: String): Resource<String> = error("unused")
        override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> = error("unused")
        override suspend fun clearSession() = error("unused")
    }

    private val crypto = VaultCryptoImpl()

    @Test
    fun resetRelocksVaultSoTheNewPasswordUnlocksIt() = runBlocking {
        val vault = crypto.createVault("Old-Password1")
        val originalVaultKey = crypto.unlockVaultKey(
            "Old-Password1", vault.encryptedKeys.masterSalt, vault.encryptedKeys.encryptedVaultKey,
        )
        val repository = RecordingAuthRepository()

        val result = ResetPasswordUseCase(repository, crypto)(
            email = "t@example.com",
            otp = "034862",
            // Pasted with a line break, like copying it off the two-line dialog.
            recoveryKey = vault.recoveryKey.chunked(30).joinToString("\n"),
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
            masterSalt = vault.encryptedKeys.masterSalt,
            encryptedVaultKeyRecovery = vault.encryptedKeys.encryptedVaultKeyRecovery,
        )

        assertTrue(result is Resource.Success)
        assertEquals("New-Password2", repository.sentPassword)
        // Login after the reset (same salt, new password) opens the same vault.
        assertEquals(
            originalVaultKey,
            crypto.unlockVaultKey("New-Password2", vault.encryptedKeys.masterSalt, repository.sentEncryptedVaultKey!!),
        )
    }

    @Test
    fun wrongRecoveryKeyNeverReachesTheServer() = runBlocking {
        val vault = crypto.createVault("Old-Password1")
        val repository = RecordingAuthRepository()

        val result = ResetPasswordUseCase(repository, crypto)(
            email = "t@example.com",
            otp = "034862",
            recoveryKey = crypto.createVault("x-other-vault").recoveryKey,
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
            masterSalt = vault.encryptedKeys.masterSalt,
            encryptedVaultKeyRecovery = vault.encryptedKeys.encryptedVaultKeyRecovery,
        )

        assertEquals(DataError.InvalidRecoveryKey, (result as Resource.Error).error)
        assertFalse(repository.called)
    }

    @Test
    fun accountWithoutVaultOnlyChangesPassword() = runBlocking {
        val repository = RecordingAuthRepository()

        val result = ResetPasswordUseCase(repository, crypto)(
            email = "t@example.com",
            otp = "034862",
            recoveryKey = "",
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
            masterSalt = "",
            encryptedVaultKeyRecovery = "",
        )

        assertTrue(result is Resource.Success)
        assertTrue(repository.called)
        assertNull(repository.sentEncryptedVaultKey)
    }
}
