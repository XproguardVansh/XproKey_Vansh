package com.xprokeey2.presentation.cards.details

import com.xprokeey2.domain.model.CardDetails
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class CardDetailsUiState(
    val user: UserBadge? = null,
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val details: CardDetails? = null,
    val isNumberRevealed: Boolean = false,
    val isCvcRevealed: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
)

/** Fields with a COPY action on the details page. */
enum class CardField { LABEL, HOLDER_NAME, NUMBER, BRAND, EXPIRY, CVC, CATEGORY, BANK_NAME, NOTES }

sealed interface CardDetailsAction {
    /** Screen became visible (first time, or back from Edit). */
    data object Refresh : CardDetailsAction
    data object ToggleNumber : CardDetailsAction
    data object ToggleCvc : CardDetailsAction
    data class Copy(val field: CardField) : CardDetailsAction
    data object DeleteClicked : CardDetailsAction
    data object DeleteDismissed : CardDetailsAction
    data object DeleteConfirmed : CardDetailsAction
}

sealed interface CardDetailsEvent {
    /** [sensitive] values (number, CVC) are hidden from clipboard previews. */
    data class CopyToClipboard(val label: UiText, val value: UiText, val sensitive: Boolean) : CardDetailsEvent
    data class ShowMessage(val message: UiText) : CardDetailsEvent
    data class Deleted(val message: UiText) : CardDetailsEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : CardDetailsEvent
}
