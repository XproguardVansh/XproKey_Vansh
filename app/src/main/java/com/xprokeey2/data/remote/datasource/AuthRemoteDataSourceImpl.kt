package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.api.AuthApi
import com.xprokeey2.data.remote.dto.auth.EmailRequestDto
import com.xprokeey2.data.remote.dto.auth.ForgotPasswordResponseDto
import com.xprokeey2.data.remote.dto.auth.LoginRequestDto
import com.xprokeey2.data.remote.dto.auth.LoginResponseDto
import com.xprokeey2.data.remote.dto.auth.ResetPasswordRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupResponseDto
import com.xprokeey2.data.remote.dto.auth.VerifyAccountRequestDto
import com.xprokeey2.data.remote.dto.common.MessageResponseDto
import com.xprokeey2.data.remote.util.safeApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val api: AuthApi,
    private val json: Json,
) : AuthRemoteDataSource {

    override suspend fun signup(request: SignupRequestDto): Resource<SignupResponseDto> =
        safeApiCall(json) { api.signup(request) }

    override suspend fun login(request: LoginRequestDto): Resource<LoginResponseDto> =
        safeApiCall(json) { api.login(request) }

    override suspend fun verifyAccount(request: VerifyAccountRequestDto): Resource<MessageResponseDto> =
        safeApiCall(json) { api.verifyAccount(request) }

    override suspend fun resendSignupOtp(email: String): Resource<MessageResponseDto> =
        safeApiCall(json) { api.resendSignupOtp(EmailRequestDto(email)) }

    override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResponseDto> =
        safeApiCall(json) { api.forgotPassword(EmailRequestDto(email)) }

    override suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<MessageResponseDto> =
        safeApiCall(json) { api.resetPassword(request) }
}
