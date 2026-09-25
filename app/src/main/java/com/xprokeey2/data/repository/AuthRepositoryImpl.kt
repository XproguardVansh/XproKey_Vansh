package com.xprokeey2.data.repository

import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toSessionEntity
import com.xprokeey2.data.remote.datasource.AuthRemoteDataSource
import com.xprokeey2.data.remote.dto.auth.LoginRequestDto
import com.xprokeey2.data.remote.dto.auth.ResetPasswordRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupRequestDto
import com.xprokeey2.data.remote.dto.auth.VerifyAccountRequestDto
import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val sessionStorage: SessionStorage,
    private val vaultSession: VaultSession,
) : AuthRepository {

    override suspend fun signup(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        vaultKeys: EncryptedVaultKeys,
    ): Resource<SignupResult> {
        val request = SignupRequestDto(
            name = name,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            masterSalt = vaultKeys.masterSalt,
            encryptedVaultKey = vaultKeys.encryptedVaultKey,
            encryptedVaultKeyRecovery = vaultKeys.encryptedVaultKeyRecovery,
        )
        return when (val result = remote.signup(request)) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error -> result
        }
    }

    override suspend fun login(email: String, password: String): Resource<LoginResult> =
        when (val result = remote.login(LoginRequestDto(email = email, password = password))) {
            is Resource.Success -> {
                sessionStorage.save(result.data.toSessionEntity())
                Resource.Success(result.data.toDomain())
            }
            is Resource.Error -> result
        }

    override suspend fun verifyAccount(email: String, otp: String): Resource<String> =
        when (val result = remote.verifyAccount(VerifyAccountRequestDto(email = email, otp = otp))) {
            is Resource.Success -> Resource.Success(result.data.message)
            is Resource.Error -> result
        }

    override suspend fun resendSignupOtp(email: String): Resource<String> =
        when (val result = remote.resendSignupOtp(email)) {
            is Resource.Success -> Resource.Success(result.data.message)
            is Resource.Error -> result
        }

    override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> =
        when (val result = remote.forgotPassword(email)) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error -> result
        }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String,
        encryptedVaultKey: String?,
    ): Resource<String> {
        val request = ResetPasswordRequestDto(
            email = email,
            otp = otp,
            password = newPassword,
            confirmPassword = confirmPassword,
            encryptedVaultKey = encryptedVaultKey,
        )
        return when (val result = remote.resetPassword(request)) {
            is Resource.Success -> Resource.Success(result.data.message)
            is Resource.Error -> result
        }
    }

    override suspend fun clearSession() {
        sessionStorage.clear()
        vaultSession.lock()
    }
}
