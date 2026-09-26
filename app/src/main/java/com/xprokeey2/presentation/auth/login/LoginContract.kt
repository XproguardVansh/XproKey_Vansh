package com.xprokeey2.presentation.auth.login

import com.xprokeey2.presentation.util.UiText

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface LoginAction {
    data class EmailChanged(val email: String) : LoginAction
    data class PasswordChanged(val password: String) : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data class RememberMeChanged(val checked: Boolean) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEvent {
    /** Setup already done (server next_action "dashboard"). */
    data object NavigateToDashboard : LoginEvent

    /** First time: choose Personal / Business, then activate. */
    data object NavigateToAccountSetup : LoginEvent
    data class NavigateToVerify(val email: String) : LoginEvent
    data class ShowMessage(val message: UiText) : LoginEvent
}
