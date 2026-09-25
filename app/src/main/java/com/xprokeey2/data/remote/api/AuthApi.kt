package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.auth.EmailRequestDto
import com.xprokeey2.data.remote.dto.auth.ForgotPasswordResponseDto
import com.xprokeey2.data.remote.dto.auth.LoginRequestDto
import com.xprokeey2.data.remote.dto.auth.LoginResponseDto
import com.xprokeey2.data.remote.dto.auth.ResetPasswordRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupRequestDto
import com.xprokeey2.data.remote.dto.auth.SignupResponseDto
import com.xprokeey2.data.remote.dto.auth.VerifyAccountRequestDto
import com.xprokeey2.data.remote.dto.common.MessageResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("signup")
    suspend fun signup(@Body body: SignupRequestDto): SignupResponseDto

    @POST("login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("verify-account")
    suspend fun verifyAccount(@Body body: VerifyAccountRequestDto): MessageResponseDto

    @POST("resend-signup-otp")
    suspend fun resendSignupOtp(@Body body: EmailRequestDto): MessageResponseDto

    @POST("forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequestDto): ForgotPasswordResponseDto

    @POST("reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto): MessageResponseDto
}
