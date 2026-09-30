package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.data.local.session.InMemoryVaultSession
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangePasswordUseCaseTest {

    private class RecordingAuthRepository(
        private val response: Resource<String> = Resource.Success("Password reset successfully"),
    ) : AuthRepository {
        var called = false
        var sentEmail: String? = null
        var sentOtp: String? = null
        var sentPassword: String? = null
        var sentConfirmPassword: String? = null
        var sentEncryptedVaultKey: String? = null

        override suspend fun resetPassword(
            email: String,
            otp: String,
            newPassword: String,
            confirmPassword: String,
            encryptedVaultKey: String?,
        ): Resource<String> {
            called = true
            sentEmail = email
            sentOtp = otp
            sentPassword = newPassword
            sentConfirmPassword = confirmPassword
            sentEncryptedVaultKey = encryptedVaultKey
            return response
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

    private class FakeSessionRepository(private val masterSalt: String?) : SessionRepository {
        var savedEncryptedVaultKey: String? = null

        override suspend fun hasSession() = true

        override suspend fun getMasterSalt(): String? = masterSalt

        override suspend fun getEncryptedVaultKey(): String? = savedEncryptedVaultKey

        override suspend fun saveEncryptedVaultKey(encryptedVaultKey: String) {
            savedEncryptedVaultKey = encryptedVaultKey
        }
    }

    private val crypto = VaultCryptoImpl()

    /** A signed-in account: its vault key unlocked in memory and its salt saved at login. */
    private suspend fun signedIn(password: String): Triple<String, String, InMemoryVaultSession> {
        val vault = crypto.createVault(password)
        val masterSalt = vault.encryptedKeys.masterSalt
        val vaultKey = crypto.unlockVaultKey(password, masterSalt, vault.encryptedKeys.encryptedVaultKey)!!
        return Triple(vaultKey, masterSalt, InMemoryVaultSession().apply { unlock(vaultKey) })
    }

    @Test
    fun changeRelocksTheSignedInVaultKeyWithTheNewPassword() = runBlocking {
        val (vaultKey, masterSalt, vaultSession) = signedIn("Old-Password1")
        val authRepository = RecordingAuthRepository()
        val sessionRepository = FakeSessionRepository(masterSalt)

        val result = ChangePasswordUseCase(authRepository, sessionRepository, crypto, vaultSession)(
            email = " t@example.com ",
            otp = " 034862 ",
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
        )

        assertEquals("Password reset successfully", (result as Resource.Success).data)
        assertEquals("t@example.com", authRepository.sentEmail)
        assertEquals("034862", authRepository.sentOtp)
        assertEquals("New-Password2", authRepository.sentPassword)
        assertEquals("New-Password2", authRepository.sentConfirmPassword)
        val sent = authRepository.sentEncryptedVaultKey!!
        // Next login (same salt, new password) opens the same vault; the old password no longer does.
        assertEquals(vaultKey, crypto.unlockVaultKey("New-Password2", masterSalt, sent))
        assertNull(crypto.unlockVaultKey("Old-Password1", masterSalt, sent))
        assertEquals(sent, sessionRepository.savedEncryptedVaultKey)
    }

    @Test
    fun failedChangeKeepsTheSavedVaultKey() = runBlocking {
        val (_, masterSalt, vaultSession) = signedIn("Old-Password1")
        val authRepository = RecordingAuthRepository(Resource.Error(DataError.Server(400, "Invalid OTP")))
        val sessionRepository = FakeSessionRepository(masterSalt)

        val result = ChangePasswordUseCase(authRepository, sessionRepository, crypto, vaultSession)(
            email = "t@example.com",
            otp = "000000",
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
        )

        assertEquals(DataError.Server(400, "Invalid OTP"), (result as Resource.Error).error)
        assertTrue(authRepository.called)
        assertNull(sessionRepository.savedEncryptedVaultKey)
    }

    @Test
    fun lockedVaultNeedsSignInWithoutCallingTheServer() = runBlocking {
        val (_, masterSalt, _) = signedIn("Old-Password1")
        val authRepository = RecordingAuthRepository()

        val result = ChangePasswordUseCase(authRepository, FakeSessionRepository(masterSalt), crypto, InMemoryVaultSession())(
            email = "t@example.com",
            otp = "034862",
            newPassword = "New-Password2",
            confirmPassword = "New-Password2",
        )

        assertEquals(DataError.VaultLocked, (result as Resource.Error).error)
        assertFalse(authRepository.called)
    }

    @Test
    fun missingSaltNeedsSignInWithoutCallingTheServer() = runBlocking {
        val (_, _, vaultSession) = signedIn("Old-Password1")
        for (salt in listOf(null, "", "  ")) {
            val authRepository = RecordingAuthRepository()

            val result = ChangePasswordUseCase(authRepository, FakeSessionRepository(salt), crypto, vaultSession)(
                email = "t@example.com",
                otp = "034862",
                newPassword = "New-Password2",
                confirmPassword = "New-Password2",
            )

            assertEquals(DataError.VaultLocked, (result as Resource.Error).error)
            assertFalse(authRepository.called)
        }
    }
}
