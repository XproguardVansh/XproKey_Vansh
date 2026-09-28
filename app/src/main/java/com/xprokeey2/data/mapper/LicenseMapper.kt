package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.license.ActivateLicenseResponseDto
import com.xprokeey2.domain.model.LicenseActivation

fun ActivateLicenseResponseDto.toDomain() = LicenseActivation(
    message = message.orEmpty(),
    organization = organization?.takeIf { it.isNotBlank() },
)
