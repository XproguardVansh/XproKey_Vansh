package com.xprokeey2.presentation.support.details

import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class SupportTicketUiState(
    val user: UserBadge? = null,
    val isLoading: Boolean = true,
    val ticket: SupportTicket? = null,
    val isRefreshing: Boolean = false,
)

sealed interface SupportTicketAction {
    data object Refresh : SupportTicketAction
    data object CopyTicketId : SupportTicketAction
}

sealed interface SupportTicketEvent {
    data class ShowMessage(val message: UiText) : SupportTicketEvent
    data class CopyToClipboard(val value: String) : SupportTicketEvent

    /** Like the web: the ticket can't be opened, so go back to the list and say why. */
    data class LoadFailed(val message: UiText) : SupportTicketEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : SupportTicketEvent
}
