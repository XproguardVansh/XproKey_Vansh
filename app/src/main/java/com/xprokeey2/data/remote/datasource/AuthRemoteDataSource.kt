package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.auth.ForgotPasswordResponseDto
import com.xprokeey2.data.remote.dto.auth.LoginRequestDto
import com.xprokeey2.data.remote.dto.auth.LoginResponseDto
import com.xprokeey2.data.remote.dto.auth.ResetPasswordRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupResponseDto
import com.xprokeey2.data.remote.dto.auth.VerifyAccountRequestDto
import com.xprokeey2.data.remote.dto.common.MessageResponseDto
import com.xprokeey2.domain.util.Resource

interface AuthRemoteDataSource {
    suspend fun signup(request: SignupRequestDto): Resource<SignupResponseDto>
    suspend fun login(request: LoginRequestDto): Resource<LoginResponseDto>
    suspend fun verifyAccount(request: VerifyAccountRequestDto): Resource<MessageResponseDto>
    suspend fun resendSignupOtp(email: String): Resource<MessageResponseDto>
    suspend fun forgotPassword(email: String): Resource<ForgotPasswordResponseDto>
    suspend fun resetPassword(request: ResetPasswordRequestDto): Resource<MessageResponseDto>
}
