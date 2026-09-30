package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.account.MeDto
import com.xprokeey2.domain.model.AccountProfile
import java.time.Instant
import java.time.OffsetDateTime

fun MeDto.toDomain() = AccountProfile(
    email = email.orEmpty(),
    name = name.orEmpty().trim(),
    accountType = accountType,
    joinedAt = (createdAt ?: createdAtCamel).toTimestamp(),
    organizationName = organizationName,
    licenseStatus = licenseStatus,
    licenseExpiresAt = licenseExpiresAt.toTimestamp(),
)

private fun String?.toTimestamp(): Instant? = this?.takeIf { it.isNotBlank() }?.let {
    runCatching { Instant.parse(it) }.getOrNull() ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
}
