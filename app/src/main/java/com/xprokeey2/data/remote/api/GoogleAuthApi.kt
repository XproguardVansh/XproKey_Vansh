package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.auth.GoogleAuthRequestDto
import com.xprokeey2.data.remote.dto.auth.GoogleAuthResponseDto
import com.xprokeey2.data.remote.dto.auth.SetupVaultPasswordRequestDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Google sign-in, made before there is a session (the web's lib/api/auth.ts googleAuth / setupVaultPassword). */
interface GoogleAuthApi {

    @POST("auth/google")
    suspend fun signIn(@Body body: GoogleAuthRequestDto): GoogleAuthResponseDto

    /** [authorization] is "Bearer <setup_token>" from POST /auth/google. */
    @POST("auth/setup-vault-password")
    suspend fun setupVaultPassword(
        @Header("Authorization") authorization: String,
        @Body body: SetupVaultPasswordRequestDto,
    ): GoogleAuthResponseDto
}
