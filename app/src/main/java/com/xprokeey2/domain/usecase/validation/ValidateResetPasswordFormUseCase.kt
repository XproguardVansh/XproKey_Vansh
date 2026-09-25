package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

data class ResetPasswordFormErrors(
    val otp: ValidationError? = null,
    val recoveryKey: ValidationError? = null,
    val newPassword: ValidationError? = null,
    val confirmPassword: ValidationError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(otp, recoveryKey, newPassword, confirmPassword).any { it != null }
}

class ValidateResetPasswordFormUseCase @Inject constructor(
    private val validateOtp: ValidateOtpUseCase,
    private val validateNewPassword: ValidateNewPasswordUseCase,
) {
    operator fun invoke(
        otp: String,
        recoveryKey: String,
        newPassword: String,
        confirmPassword: String,
        recoveryKeyRequired: Boolean,
    ) = ResetPasswordFormErrors(
        otp = if (otp.isBlank()) ValidationError.REQUIRED else validateOtp(otp.trim()),
        recoveryKey = if (recoveryKeyRequired && recoveryKey.isBlank()) ValidationError.REQUIRED else null,
        newPassword = validateNewPassword(newPassword),
        confirmPassword = when {
            confirmPassword.isEmpty() -> ValidationError.REQUIRED
            confirmPassword != newPassword -> ValidationError.PASSWORD_MISMATCH
            else -> null
        },
    )
}
