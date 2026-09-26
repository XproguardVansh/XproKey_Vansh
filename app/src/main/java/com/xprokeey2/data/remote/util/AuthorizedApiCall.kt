package com.xprokeey2.data.remote.util

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection

/**
 * [safeApiCall] for endpoints behind the login session: [call] gets "Bearer <access token>".
 * An expired access token is refreshed by the OkHttp authenticator, so a 401 that still comes
 * back means the whole session is over.
 */
suspend fun <T> authorizedApiCall(
    tokenStore: AccessTokenStore,
    json: Json,
    call: suspend (authorization: String) -> T,
): Resource<T> {
    val accessToken = tokenStore.getAccessToken()
    if (accessToken.isNullOrBlank()) return Resource.Error(DataError.SessionExpired)

    return when (val result = safeApiCall(json) { call("Bearer $accessToken") }) {
        is Resource.Success -> result
        is Resource.Error -> {
            val error = result.error
            if (error is DataError.Server && error.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Resource.Error(DataError.SessionExpired)
            } else {
                result
            }
        }
    }
}
