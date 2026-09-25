package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.model.User
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {

    private class FakeAuthRepository(private val loginResult: LoginResult) : AuthRepository {
        var sessionCleared = false

        override suspend fun login(email: String, password: String) = Resource.Success(loginResult)
        override suspend fun clearSession() {
            sessionCleared = true
        }

        override suspend fun signup(
            name: String,
            email: String,
            password: String,
            confirmPassword: String,
            vaultKeys: EncryptedVaultKeys,
        ): Resource<SignupResult> = error("unused")

        override suspend fun verifyAccount(email: String, otp: String): Resource<String> = error("unused")
        override suspend fun resendSignupOtp(email: String): Resource<String> = error("unused")
        override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> = error("unused")
        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmPassword: String,
            encryptedVaultKey: String?,
        ): Resource<String> = error("unused")
    }

    private class FakeVaultSession : VaultSession {
        override var vaultKey: String? = "stale"
        override fun unlock(vaultKey: String) {
            this.vaultKey = vaultKey
        }

        override fun lock() {
            vaultKey = null
        }
    }

    private fun loginResult(masterSalt: String, encryptedVaultKey: String) = LoginResult(
        message = "Login successful",
        user = User(userId = "0519", name = "Test", email = "t@example.com"),
        nextAction = "dashboard",
        masterSalt = masterSalt,
        encryptedVaultKey = encryptedVaultKey,
    )

    @Test
    fun accountWithoutVaultKeysStillSignsIn() = runBlocking {
        // What the server returns for accounts created before vault keys existed.
        val repository = FakeAuthRepository(loginResult(masterSalt = "", encryptedVaultKey = ""))
        val session = FakeVaultSession()

        val result = LoginUseCase(repository, VaultCryptoImpl(), session)("t@example.com", "Vansh@Vansh")

        assertTrue(result is Resource.Success)
        assertFalse((result as Resource.Success).data.hasVaultKeys)
        assertFalse(repository.sessionCleared)
        assertNull(session.vaultKey)
    }

    @Test
    fun validVaultKeysAreUnlocked() = runBlocking {
        val crypto = VaultCryptoImpl()
        val vault = crypto.createVault("Vansh@Vansh")
        val repository = FakeAuthRepository(
            loginResult(vault.encryptedKeys.masterSalt, vault.encryptedKeys.encryptedVaultKey)
        )
        val session = FakeVaultSession()

        val result = LoginUseCase(repository, crypto, session)("t@example.com", "Vansh@Vansh")

        assertTrue(result is Resource.Success)
        assertEquals(
            crypto.unlockVaultKey("Vansh@Vansh", vault.encryptedKeys.masterSalt, vault.encryptedKeys.encryptedVaultKey),
            session.vaultKey,
        )
    }

    @Test
    fun undecryptableVaultKeyFailsAndClearsSession() = runBlocking {
        val crypto = VaultCryptoImpl()
        val vault = crypto.createVault("a-different-password")
        val repository = FakeAuthRepository(
            loginResult(vault.encryptedKeys.masterSalt, vault.encryptedKeys.encryptedVaultKey)
        )

        val result = LoginUseCase(repository, crypto, FakeVaultSession())("t@example.com", "Vansh@Vansh")

        assertEquals(DataError.VaultUnlockFailed, (result as Resource.Error).error)
        assertTrue(repository.sessionCleared)
    }
}
