package com.xprokeey2.data.remote.auth

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.TokenApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Access tokens live about 15 minutes. When an authorised request comes back 401, this swaps the
 * refresh token for a new access token (POST /refresh) and retries the request once. If the server
 * rejects the refresh token too (it lasts 7 days), the saved session is cleared, the 401 goes through,
 * and the app sends the user to Login. A refresh that fails for another reason (e.g. no network)
 * keeps the session.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: AccessTokenStore,
    private val tokenApi: TokenApi,
) : Authenticator {

    private val refreshLock = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Not an authorised call (e.g. wrong password on /login), or the retry failed as well.
        val failedAuthorization = response.request.header(AUTHORIZATION) ?: return null
        if (response.priorResponse != null) return null

        // OkHttp calls this on its own background thread, so blocking here is expected.
        val accessToken = runBlocking { freshAccessToken(failedAuthorization.removePrefix(BEARER)) }
            ?: return null
        return response.request.newBuilder()
            .header(AUTHORIZATION, BEARER + accessToken)
            .build()
    }

    /**
     * One refresh at a time: requests that failed with a token another request already replaced
     * just reuse the new one. Null when there is no refresh token or the server rejects it.
     */
    internal suspend fun freshAccessToken(failedToken: String): String? = refreshLock.withLock {
        val current = tokenStore.getAccessToken()
        if (!current.isNullOrBlank() && current != failedToken) return@withLock current

        val refreshToken = tokenStore.getRefreshToken()
        if (refreshToken.isNullOrBlank()) return@withLock null

        val newToken = try {
            tokenApi.refresh(BEARER + refreshToken).accessToken
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == HttpURLConnection.HTTP_UNAUTHORIZED || e.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                tokenStore.clearSession()
            }
            null
        } catch (e: Exception) {
            null
        }
        newToken?.takeIf { it.isNotBlank() }?.also { tokenStore.saveAccessToken(it) }
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer "
    }
}
