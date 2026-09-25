package com.xprokeey2.presentation.auth.verify

import com.xprokeey2.domain.usecase.validation.ValidateOtpUseCase
import com.xprokeey2.presentation.util.UiText

data class VerifyEmailUiState(
    val email: String,
    val otp: String = "",
    val otpError: UiText? = null,
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val resendSecondsLeft: Int = 0,
    /** Non-null while the "Save your recovery key" dialog is showing. */
    val recoveryKey: String? = null,
) {
    val canVerify: Boolean get() = otp.length == ValidateOtpUseCase.OTP_LENGTH && !isVerifying
    val canResend: Boolean get() = resendSecondsLeft == 0 && !isResending
}

sealed interface VerifyEmailAction {
    data class OtpChanged(val otp: String) : VerifyEmailAction
    data object Verify : VerifyEmailAction
    data object Resend : VerifyEmailAction
    data object RecoveryKeySaved : VerifyEmailAction
}

sealed interface VerifyEmailEvent {
    data class NavigateToLogin(val email: String) : VerifyEmailEvent
    data class ShowMessage(val message: UiText) : VerifyEmailEvent
}
