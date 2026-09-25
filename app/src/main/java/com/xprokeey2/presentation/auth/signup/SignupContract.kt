package com.xprokeey2.presentation.auth.signup

import com.xprokeey2.domain.model.PasswordStrength
import com.xprokeey2.presentation.util.UiText

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    /** Null while the password field is empty. */
    val passwordStrength: PasswordStrength? = null,
    val acceptedTerms: Boolean = false,
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val termsError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface SignupAction {
    data class NameChanged(val name: String) : SignupAction
    data class EmailChanged(val email: String) : SignupAction
    data class PasswordChanged(val password: String) : SignupAction
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignupAction
    data object TogglePasswordVisibility : SignupAction
    data object ToggleConfirmPasswordVisibility : SignupAction
    data class AcceptedTermsChanged(val accepted: Boolean) : SignupAction
    data object Submit : SignupAction
}

sealed interface SignupEvent {
    data class NavigateToVerify(val email: String) : SignupEvent
    data class ShowMessage(val message: UiText) : SignupEvent
}
