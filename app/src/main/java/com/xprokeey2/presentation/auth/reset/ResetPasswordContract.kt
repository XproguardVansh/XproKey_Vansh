package com.xprokeey2.presentation.auth.reset

import com.xprokeey2.presentation.util.UiText

data class ResetPasswordUiState(
    val email: String,
    /** False for accounts without a vault: there is no recovery key to ask for. */
    val requiresRecoveryKey: Boolean,
    val otp: String = "",
    val recoveryKey: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val otpError: UiText? = null,
    val recoveryKeyError: UiText? = null,
    val newPasswordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface ResetPasswordAction {
    data class OtpChanged(val otp: String) : ResetPasswordAction
    data class RecoveryKeyChanged(val recoveryKey: String) : ResetPasswordAction
    data class NewPasswordChanged(val password: String) : ResetPasswordAction
    data class ConfirmPasswordChanged(val password: String) : ResetPasswordAction
    data object ToggleNewPasswordVisibility : ResetPasswordAction
    data object ToggleConfirmPasswordVisibility : ResetPasswordAction
    data object Submit : ResetPasswordAction
}

sealed interface ResetPasswordEvent {
    data class PasswordReset(val email: String, val message: String) : ResetPasswordEvent
    data class ShowMessage(val message: UiText) : ResetPasswordEvent
}
