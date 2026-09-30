package com.xprokeey2.data.remote.auth

import com.xprokeey2.data.remote.dto.common.ErrorResponseDto
import com.xprokeey2.domain.model.AccessBlock
import com.xprokeey2.domain.security.AccessGate
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches every response for the server's access guard, HTTP 403 with `next_action`, which the web's API
 * client turns into a redirect to checkout or license activation. /payments isn't behind the guard, and
 * its own 403s (e.g. "Trial already used") are answers for the checkout screen, so they're left alone.
 */
@Singleton
class AccessGuardInterceptor @Inject constructor(
    private val json: Json,
) : Interceptor, AccessGate {

    private val _blocks = MutableSharedFlow<AccessBlock>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val blocks: Flow<AccessBlock> = _blocks.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == HttpURLConnection.HTTP_FORBIDDEN && !chain.request().url.isPaymentsEndpoint()) {
            accessBlockOf(json, response.peekBody(MAX_ERROR_BODY_BYTES).string())?.let { _blocks.tryEmit(it) }
        }
        return response
    }

    private companion object {
        const val MAX_ERROR_BODY_BYTES = 16L * 1024
    }
}

/** The block a 403 body describes, e.g. `{"next_action": "trial_expired"}`; null for any other 403. */
internal fun accessBlockOf(json: Json, body: String): AccessBlock? =
    runCatching { json.decodeFromString<ErrorResponseDto>(body) }.getOrNull()?.let { AccessBlock.of(it.nextAction) }

private fun HttpUrl.isPaymentsEndpoint(): Boolean = pathSegments.firstOrNull() == "payments"
