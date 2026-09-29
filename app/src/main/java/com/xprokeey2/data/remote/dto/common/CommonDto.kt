package com.xprokeey2.data.remote.dto.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponseDto(
    val message: String,
)

/**
 * Error body returned by the backend, e.g.
 * `{"error": "Invalid email or password"}` or
 * `{"error": "Account is not verified. ...", "is_verified": false}`.
 */
@Serializable
data class ErrorResponseDto(
    val error: String? = null,
    /** Some endpoints (e.g. support tickets) explain errors in "message" instead. */
    val message: String? = null,
    val detail: String? = null,
    @SerialName("is_verified") val isVerified: Boolean? = null,
)

/** Envelope of the card endpoints: `{"data": ..., "message": "..."}`. */
@Serializable
data class DataResponseDto<T>(
    val data: T,
    val message: String? = null,
)
