package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.support.CreateTicketRequestDto
import com.xprokeey2.data.remote.dto.support.SupportTicketDto
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketDraft
import java.time.Instant
import java.time.OffsetDateTime

fun SupportTicketDto.toDomain() = SupportTicket(
    id = id.content,
    ticketNumber = ticketNumber,
    name = name.orEmpty(),
    email = email.orEmpty(),
    category = category.orEmpty(),
    subject = subject.orEmpty(),
    message = message.orEmpty(),
    priority = priority.orEmpty(),
    status = status.orEmpty(),
    createdAt = createdAt.toTimestamp(),
    updatedAt = updatedAt.toTimestamp(),
)

fun TicketDraft.toRequestDto() = CreateTicketRequestDto(
    name = name,
    email = email,
    category = category.apiValue,
    subject = subject,
    message = message,
    priority = priority.apiValue,
)

/** "2026-09-29T11:33:00Z", or with an offset such as "+05:30"; null for anything else. */
private fun String?.toTimestamp(): Instant? = this?.let {
    runCatching { Instant.parse(it) }.getOrNull() ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
}
