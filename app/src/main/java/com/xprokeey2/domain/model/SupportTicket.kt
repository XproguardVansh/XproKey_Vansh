package com.xprokeey2.domain.model

import java.time.Instant

/**
 * A support ticket as GET /support/my-tickets and /support/tickets/{id} return it. [status],
 * [category] and [priority] are kept as the server sends them (the web filters on the raw values
 * and only maps them for display, see [TicketStatus.of] and friends).
 */
data class SupportTicket(
    val id: String,
    val ticketNumber: String?,
    val name: String,
    val email: String,
    val category: String,
    val subject: String,
    val message: String,
    val priority: String,
    val status: String,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    /** Web: `ticket_number || "TICK-" + id padded to 4 digits`, e.g. TICK-0014. */
    val displayId: String get() = ticketNumber?.takeIf { it.isNotEmpty() } ?: "TICK-" + id.padStart(4, '0')

    /**
     * The web list's search: subject, message, or the ticket number (the id when there is none),
     * ignoring case.
     */
    fun matchesSearch(query: String): Boolean {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) return true
        return subject.lowercase().contains(needle) ||
            message.lowercase().contains(needle) ||
            (ticketNumber?.takeIf { it.isNotEmpty() } ?: id).lowercase().contains(needle)
    }
}

/** A new ticket from the "Submit a Support Ticket" form (POST /support/tickets). */
data class TicketDraft(
    val name: String,
    val email: String,
    val category: TicketCategory,
    val subject: String,
    val message: String,
    /** The web form always sends "medium". */
    val priority: TicketPriority = TicketPriority.MEDIUM,
)

/** POST /support/tickets result: the server's message, and the new ticket when it sends one. */
data class CreatedTicket(val message: String?, val ticket: SupportTicket?)

enum class TicketStatus(val apiValue: String) {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    RESOLVED("resolved"),
    CLOSED("closed");

    companion object {
        /** Web `formatStatus`: anything unknown shows as Open. */
        fun of(raw: String?): TicketStatus = entries.firstOrNull { it.apiValue == raw?.lowercase() } ?: OPEN
    }
}

enum class TicketCategory(val apiValue: String) {
    TECHNICAL("technical"),
    BILLING("billing"),
    ACCOUNT("account"),
    BUG_REPORT("bug_report"),
    GENERAL("general");

    companion object {
        /** Web `formatCategory`: anything unknown shows as General. */
        fun of(raw: String?): TicketCategory = entries.firstOrNull { it.apiValue == raw?.lowercase() } ?: GENERAL
    }
}

enum class TicketPriority(val apiValue: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    companion object {
        /** Web `formatPriority`: anything unknown shows as Medium. */
        fun of(raw: String?): TicketPriority = entries.firstOrNull { it.apiValue == raw?.lowercase() } ?: MEDIUM
    }
}

/** The web page's counters: resolved includes closed tickets. */
data class TicketStats(val total: Int, val open: Int, val inProgress: Int, val resolved: Int) {
    companion object {
        fun of(tickets: List<SupportTicket>): TicketStats {
            fun count(vararg statuses: TicketStatus) =
                tickets.count { ticket -> statuses.any { it.apiValue == ticket.status.lowercase() } }
            return TicketStats(
                total = tickets.size,
                open = count(TicketStatus.OPEN),
                inProgress = count(TicketStatus.IN_PROGRESS),
                resolved = count(TicketStatus.RESOLVED, TicketStatus.CLOSED),
            )
        }
    }
}
