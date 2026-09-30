package com.xprokeey2.data.local.session

/** The login tokens, as needed by authorised API calls and the token refresh. */
interface AccessTokenStore {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveAccessToken(accessToken: String)

    /** The server rejected the refresh token: the saved session is over. */
    suspend fun clearSession()
}
