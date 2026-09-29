package com.xprokeey2.data.remote.dto.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** lib/api/support.ts `SupportTicket`; [id] may be a number or a string. */
@Serializable
data class SupportTicketDto(
    val id: JsonPrimitive,
    @SerialName("ticket_number") val ticketNumber: String? = null,
    val name: String? = null,
    val email: String? = null,
    val category: String? = null,
    val subject: String? = null,
    val message: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** `{"tickets": [...], "total": 1}` from GET /support/my-tickets. */
@Serializable
data class SupportTicketListDto(
    val tickets: List<SupportTicketDto>? = null,
    val total: Int? = null,
)

/** `{"ticket": {...}}` from GET /support/tickets/{id}. */
@Serializable
data class SupportTicketResponseDto(
    val ticket: SupportTicketDto,
)

/** Body of POST /support/tickets. */
@Serializable
data class CreateTicketRequestDto(
    val name: String,
    val email: String,
    val category: String,
    val subject: String,
    val message: String,
    val priority: String,
)

/** `{"message": "...", "ticket": {...}}` */
@Serializable
data class CreateTicketResponseDto(
    val message: String? = null,
    val ticket: SupportTicketDto? = null,
)
