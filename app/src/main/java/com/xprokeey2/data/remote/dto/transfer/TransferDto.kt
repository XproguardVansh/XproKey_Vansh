package com.xprokeey2.data.remote.dto.transfer

import kotlinx.serialization.Serializable

/** One entry of the POST /import body, which is a plain JSON array of these (as the web sends it). */
@Serializable
data class ImportItemDto(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
)

/** `{"message": "...", "imported": 8}` */
@Serializable
data class ImportResponseDto(
    val message: String? = null,
    val imported: Int? = null,
)
