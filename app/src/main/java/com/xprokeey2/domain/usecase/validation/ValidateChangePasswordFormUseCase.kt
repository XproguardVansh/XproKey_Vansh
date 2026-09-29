package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

/** Why the Change password form can't be sent; the page shows one at a time, like the web. */
enum class ChangePasswordFormError {
    OTP_REQUIRED,
    OTP_WRONG_LENGTH,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
}

/** The web page's checks, in its order. */
class ValidateChangePasswordFormUseCase @Inject constructor() {

    /** Returns the first failed check, or null when the form can be sent. */
    operator fun invoke(otp: String, newPassword: String, confirmPassword: String): ChangePasswordFormError? {
        val code = otp.trim()
        return when {
            code.isEmpty() -> ChangePasswordFormError.OTP_REQUIRED
            code.length != ValidateOtpUseCase.OTP_LENGTH -> ChangePasswordFormError.OTP_WRONG_LENGTH
            newPassword.isEmpty() -> ChangePasswordFormError.PASSWORD_REQUIRED
            newPassword.length < ValidateNewPasswordUseCase.MIN_LENGTH -> ChangePasswordFormError.PASSWORD_TOO_SHORT
            newPassword != confirmPassword -> ChangePasswordFormError.PASSWORD_MISMATCH
            else -> null
        }
    }
}
