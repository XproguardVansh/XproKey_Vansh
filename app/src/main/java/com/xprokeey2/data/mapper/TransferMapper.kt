package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.transfer.ImportItemDto
import com.xprokeey2.domain.model.ImportRecord

fun ImportRecord.toDto() = ImportItemDto(
    title = title,
    username = username,
    password = password,
    url = url,
    notes = notes,
    category = category,
)
