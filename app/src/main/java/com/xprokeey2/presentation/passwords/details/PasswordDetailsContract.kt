package com.xprokeey2.presentation.passwords.details

import com.xprokeey2.domain.model.VaultItemDetails
import com.xprokeey2.presentation.util.UiText

data class PasswordDetailsUiState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val details: VaultItemDetails? = null,
    val isPasswordVisible: Boolean = false,
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
)

/** Fields with a Copy button on the details page. */
enum class VaultField { USERNAME, WEBSITE, PASSWORD, NOTES }

sealed interface PasswordDetailsAction {
    /** Screen became visible (first time, or back from Edit). */
    data object Refresh : PasswordDetailsAction
    data object TogglePassword : PasswordDetailsAction
    data class Copy(val field: VaultField) : PasswordDetailsAction
    data object DeleteClicked : PasswordDetailsAction
    data object DeleteDismissed : PasswordDetailsAction
    data object DeleteConfirmed : PasswordDetailsAction
}

sealed interface PasswordDetailsEvent {
    /** The password is flagged sensitive so Android 13+ doesn't preview it. */
    data class CopyToClipboard(val label: UiText, val value: String, val sensitive: Boolean) : PasswordDetailsEvent
    data class ShowMessage(val message: UiText) : PasswordDetailsEvent
    data class Deleted(val message: UiText) : PasswordDetailsEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : PasswordDetailsEvent
}
