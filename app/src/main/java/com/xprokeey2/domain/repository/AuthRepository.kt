package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.util.Resource

interface AuthRepository {

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        vaultKeys: EncryptedVaultKeys,
    ): Resource<SignupResult>

    /** On success the session (tokens + encrypted vault key material) is persisted. */
    suspend fun login(email: String, password: String): Resource<LoginResult>

    /** Returns the server message. */
    suspend fun verifyAccount(email: String, otp: String): Resource<String>

    /** Returns the server message. */
    suspend fun resendSignupOtp(email: String): Resource<String>

    /** Emails a reset OTP and returns the recovery-encrypted vault key needed for the reset. */
    suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult>

    /**
     * Sets a new password. [encryptedVaultKey] is the vault key re-locked with the new password
     * (null for accounts without a vault). Returns the server message.
     */
    suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String,
        encryptedVaultKey: String?,
    ): Resource<String>

    suspend fun clearSession()
}
