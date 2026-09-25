package com.xprokeey2.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class VerifyAccountRequestDto(
    val email: String,
    val otp: String,
)

/** Body for /resend-signup-otp. */
@Serializable
data class EmailRequestDto(
    val email: String,
)
