package com.xprokeey2.data.remote.dto.vault

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A vault item as every vault endpoint returns it. [password] (vault-key ciphertext) is only
 * included by getvaultbyid.
 */
@Serializable
data class VaultItemDto(
    val id: Long,
    val title: String? = null,
    val username: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val category: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean? = null,
    val password: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** `{"item": {...}, "message": "..."}` from createvault and getvaultbyid. */
@Serializable
data class VaultItemResponseDto(
    val item: VaultItemDto,
    val message: String? = null,
)

/** `{"items": [...]}` from getallvault. */
@Serializable
data class VaultItemListDto(
    val items: List<VaultItemDto> = emptyList(),
)

/** Body of POST /vault/createvault. */
@Serializable
data class VaultItemRequestDto(
    val title: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
    val category: String,
    @SerialName("is_favorite") val isFavorite: Boolean,
)

/** Body of PATCH /vault/updatevault/:id: only changed fields (nulls are left out of the JSON). */
@Serializable
data class VaultItemUpdateRequestDto(
    val title: String? = null,
    val username: String? = null,
    val password: String? = null,
    val url: String? = null,
    val notes: String? = null,
    val category: String? = null,
    @SerialName("is_favorite") val isFavorite: Boolean? = null,
)

/** `{"categories": ["Personal", "Work", ...]}` from GET /vault/categories. */
@Serializable
data class VaultCategoriesDto(
    val categories: List<String> = emptyList(),
)

@Serializable
data class CustomCategoryDto(
    val id: Long? = null,
    val name: String,
)

/** `{"categories": [{"id", "user_id", "name", ...}]}` from GET /vault/categories/custom. */
@Serializable
data class CustomCategoriesDto(
    val categories: List<CustomCategoryDto> = emptyList(),
)

@Serializable
data class CreateCategoryRequestDto(
    val name: String,
)

/** `{"category": {"id", "name", ...}, "message": "Category created successfully"}` */
@Serializable
data class CreateCategoryResponseDto(
    val category: CustomCategoryDto,
    val message: String? = null,
)
