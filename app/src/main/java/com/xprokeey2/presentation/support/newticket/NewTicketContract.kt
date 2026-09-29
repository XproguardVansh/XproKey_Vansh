package com.xprokeey2.presentation.support.newticket

import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.presentation.util.UiText

/** "Submit a Support Ticket"; name and email start from the signed-in account, like the web. */
data class NewTicketUiState(
    val name: String = "",
    val email: String = "",
    val category: TicketCategory = TicketCategory.TECHNICAL,
    val subject: String = "",
    val message: String = "",
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val subjectError: UiText? = null,
    val messageError: UiText? = null,
    val isSubmitting: Boolean = false,
)

sealed interface NewTicketAction {
    data class NameChanged(val value: String) : NewTicketAction
    data class EmailChanged(val value: String) : NewTicketAction
    data class CategorySelected(val category: TicketCategory) : NewTicketAction
    data class SubjectChanged(val value: String) : NewTicketAction
    data class MessageChanged(val value: String) : NewTicketAction
    data object Submit : NewTicketAction
}

sealed interface NewTicketEvent {
    /** Sent: open [ticketId] (when the server returned it) and show [message]. */
    data class Created(val ticketId: String?, val message: UiText) : NewTicketEvent

    data class ShowMessage(val message: UiText) : NewTicketEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : NewTicketEvent
}
