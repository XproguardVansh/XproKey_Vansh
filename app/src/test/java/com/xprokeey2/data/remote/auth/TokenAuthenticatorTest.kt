package com.xprokeey2.data.remote.auth

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.TokenApi
import com.xprokeey2.data.remote.dto.auth.RefreshTokenResponseDto
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class TokenAuthenticatorTest {

    private class FakeTokenStore(var accessToken: String?, var refreshToken: String?) : AccessTokenStore {
        override suspend fun getAccessToken() = accessToken
        override suspend fun getRefreshToken() = refreshToken
        override suspend fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }
    }

    private class FakeTokenApi(private val newToken: String?) : TokenApi {
        var calls = 0
        var lastAuthorization: String? = null

        override suspend fun refresh(authorization: String): RefreshTokenResponseDto {
            calls++
            lastAuthorization = authorization
            return RefreshTokenResponseDto(accessToken = newToken ?: throw IOException("401 Invalid refresh token"))
        }
    }

    private fun unauthorized(authorization: String?, retried: Boolean = false): Response {
        val request = Request.Builder()
            .url("https://api.xprokey.com/cards/getallcard")
            .apply { if (authorization != null) header("Authorization", authorization) }
            .build()
        fun response(prior: Response?) = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .priorResponse(prior)
            .build()
        return response(if (retried) response(null) else null)
    }

    @Test
    fun refreshesExpiredTokenAndRetries() {
        val store = FakeTokenStore(accessToken = "old", refreshToken = "refresh")
        val api = FakeTokenApi(newToken = "new")

        val retry = TokenAuthenticator(store, api).authenticate(null, unauthorized("Bearer old"))

        assertEquals("Bearer new", retry?.header("Authorization"))
        assertEquals("Bearer refresh", api.lastAuthorization)
        assertEquals("new", store.accessToken)
    }

    @Test
    fun reusesTokenAlreadyRefreshedByAnotherRequest() {
        val store = FakeTokenStore(accessToken = "new", refreshToken = "refresh")
        val api = FakeTokenApi(newToken = "newer")

        val retry = TokenAuthenticator(store, api).authenticate(null, unauthorized("Bearer old"))

        assertEquals("Bearer new", retry?.header("Authorization"))
        assertEquals(0, api.calls)
    }

    @Test
    fun givesUpWhenRefreshTokenIsRejected() {
        val store = FakeTokenStore(accessToken = "old", refreshToken = "expired")
        assertNull(TokenAuthenticator(store, FakeTokenApi(newToken = null)).authenticate(null, unauthorized("Bearer old")))
        assertEquals("old", store.accessToken)
    }

    @Test
    fun ignoresLoginFailuresAndSecondAttempts() {
        val store = FakeTokenStore(accessToken = "old", refreshToken = "refresh")
        val api = FakeTokenApi(newToken = "new")
        val authenticator = TokenAuthenticator(store, api)

        assertNull(authenticator.authenticate(null, unauthorized(authorization = null)))
        assertNull(authenticator.authenticate(null, unauthorized("Bearer old", retried = true)))
        assertEquals(0, api.calls)
    }
}
