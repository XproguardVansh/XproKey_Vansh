package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.vault.VaultItemDto
import com.xprokeey2.data.remote.dto.vault.VaultItemRequestDto
import com.xprokeey2.data.remote.dto.vault.VaultItemUpdateRequestDto
import com.xprokeey2.domain.model.StoredVaultItem
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemPayload
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import java.time.Instant

fun VaultItemDto.toDomain() = VaultItem(
    id = id,
    title = title.orEmpty(),
    username = username.orEmpty(),
    url = url.orEmpty(),
    notes = notes.orEmpty(),
    category = category.orEmpty(),
    isFavorite = isFavorite ?: false,
    createdAt = createdAt.toInstantOrNull(),
    updatedAt = updatedAt.toInstantOrNull(),
)

fun VaultItemDto.toStoredItem() = StoredVaultItem(
    item = toDomain(),
    encryptedPassword = password.orEmpty(),
)

fun VaultItemPayload.toRequestDto() = VaultItemRequestDto(
    title = title,
    username = username,
    password = encryptedPassword,
    url = url,
    notes = notes,
    category = category,
    isFavorite = isFavorite,
)

fun VaultItemUpdatePayload.toRequestDto() = VaultItemUpdateRequestDto(
    title = title,
    username = username,
    password = encryptedPassword,
    url = url,
    notes = notes,
    category = category,
    isFavorite = isFavorite,
)

/** Server timestamps look like "2026-09-28T10:51:07.080231Z". */
private fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
