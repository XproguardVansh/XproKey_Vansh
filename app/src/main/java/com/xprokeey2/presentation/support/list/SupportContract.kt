package com.xprokeey2.presentation.support.list

import androidx.annotation.StringRes
import com.xprokeey2.R
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.domain.model.TicketStats
import com.xprokeey2.domain.model.TicketStatus
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class SupportUiState(
    val user: UserBadge? = null,
    /** First load only; later refreshes keep the list on screen. */
    val isLoading: Boolean = true,
    val tickets: List<SupportTicket> = emptyList(),
    val loadError: UiText? = null,
    val query: String = "",
    /** Status tab; null = All. */
    val statusFilter: TicketStatus? = null,
    /** Category menu; null = All Categories. */
    val categoryFilter: TicketCategory? = null,
) {
    val stats: TicketStats get() = TicketStats.of(tickets)

    /** The web list: tab and category compare the server's values, then the search. */
    val visibleTickets: List<SupportTicket>
        get() = tickets.filter { ticket ->
            (statusFilter == null || ticket.status.lowercase() == statusFilter.apiValue) &&
                (categoryFilter == null || ticket.category.lowercase() == categoryFilter.apiValue) &&
                ticket.matchesSearch(query)
        }

    /** Which empty message the web shows. */
    val emptyState: SupportEmptyState
        get() = when {
            tickets.isEmpty() -> SupportEmptyState.NO_TICKETS
            statusFilter == TicketStatus.IN_PROGRESS -> SupportEmptyState.NONE_IN_PROGRESS
            statusFilter == TicketStatus.CLOSED -> SupportEmptyState.NONE_CLOSED
            else -> SupportEmptyState.NO_MATCH
        }
}

enum class SupportEmptyState(@param:StringRes val title: Int, @param:StringRes val body: Int, @param:StringRes val button: Int) {
    NO_TICKETS(R.string.support_empty_none_title, R.string.support_empty_none_body, R.string.support_create_first),
    NONE_IN_PROGRESS(R.string.support_empty_in_progress_title, R.string.support_empty_in_progress_body, R.string.support_new_ticket),
    NONE_CLOSED(R.string.support_empty_closed_title, R.string.support_empty_closed_body, R.string.support_new_ticket),
    NO_MATCH(R.string.support_empty_filter_title, R.string.support_empty_filter_body, R.string.support_new_ticket),
}

sealed interface SupportAction {
    data object Refresh : SupportAction
    data class QueryChanged(val query: String) : SupportAction
    data class StatusSelected(val status: TicketStatus?) : SupportAction
    data class CategorySelected(val category: TicketCategory?) : SupportAction
}

sealed interface SupportEvent {
    data class ShowMessage(val message: UiText) : SupportEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : SupportEvent
}
