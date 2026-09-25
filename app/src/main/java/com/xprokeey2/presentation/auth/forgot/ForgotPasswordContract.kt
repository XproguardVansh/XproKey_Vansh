package com.xprokeey2.presentation.auth.forgot

import com.xprokeey2.presentation.util.UiText

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface ForgotPasswordAction {
    data class EmailChanged(val email: String) : ForgotPasswordAction
    data object Submit : ForgotPasswordAction
}

sealed interface ForgotPasswordEvent {
    data class NavigateToReset(
        val email: String,
        val masterSalt: String,
        val encryptedVaultKeyRecovery: String,
    ) : ForgotPasswordEvent
    data class ShowMessage(val message: UiText) : ForgotPasswordEvent
}
