package com.xprokeey2.domain.model

data class LicenseActivation(
    val message: String,
    /** Organization the license belongs to, when the server returns it. */
    val organization: String?,
)
