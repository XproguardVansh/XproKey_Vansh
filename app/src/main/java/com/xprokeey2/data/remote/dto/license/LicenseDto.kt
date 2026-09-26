package com.xprokeey2.data.remote.dto.license

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of POST /license/activate (sent with the user's Bearer access token). */
@Serializable
data class ActivateLicenseRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("key_code") val keyCode: String,
)

/** API doc: {"message": "License activated successfully", "organization": "Acme Corp"}. */
@Serializable
data class ActivateLicenseResponseDto(
    val message: String? = null,
    val organization: String? = null,
)
