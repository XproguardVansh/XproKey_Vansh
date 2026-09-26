package com.xprokeey2.data.remote.api

import com.xprokeey2.data.remote.dto.auth.RefreshTokenResponseDto
import retrofit2.http.Header
import retrofit2.http.POST

/** Served by its own OkHttp client, without [com.xprokeey2.data.remote.auth.TokenAuthenticator]. */
interface TokenApi {

    /** [authorization] is "Bearer <refresh token>"; there is no body. */
    @POST("refresh")
    suspend fun refresh(@Header("Authorization") authorization: String): RefreshTokenResponseDto
}
