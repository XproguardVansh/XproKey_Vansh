package com.xprokeey2.presentation.settings.changepassword

import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/**
 * Settings > Change password, like the web page: step 1 sends an OTP to the account's email,
 * step 2 ([isOtpSent]) takes the OTP and the new password.
 */
data class ChangePasswordUiState(
    val user: UserBadge? = null,
    val email: String = "",
    val isOtpSent: Boolean = false,
    val isSendingOtp: Boolean = false,
    val otp: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    /** The web's error box above the buttons; editing any field clears it. */
    val error: UiText? = null,
    val isSubmitting: Boolean = false,
)

sealed interface ChangePasswordAction {
    data object SendOtp : ChangePasswordAction
    data class OtpChanged(val otp: String) : ChangePasswordAction
    data class PasswordChanged(val password: String) : ChangePasswordAction
    data class ConfirmPasswordChanged(val password: String) : ChangePasswordAction
    data object TogglePasswordVisibility : ChangePasswordAction
    data object ToggleConfirmPasswordVisibility : ChangePasswordAction

    /** Step 2's Back button: returns to step 1 with an empty form. */
    data object Back : ChangePasswordAction
    data object Submit : ChangePasswordAction
}

sealed interface ChangePasswordEvent {
    data class ShowMessage(val message: UiText) : ChangePasswordEvent

    /** Done: leave the page and show [message]. */
    data class PasswordChanged(val message: UiText) : ChangePasswordEvent

    /** Vault locked or session over: back to Login. */
    data class SignInRequired(val message: UiText) : ChangePasswordEvent
}
