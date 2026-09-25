package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

class ValidateOtpUseCase @Inject constructor() {

    /** Returns null when [otp] is exactly [OTP_LENGTH] digits. */
    operator fun invoke(otp: String): ValidationError? =
        if (otp.length == OTP_LENGTH && otp.all(Char::isDigit)) null else ValidationError.INVALID_OTP

    companion object {
        const val OTP_LENGTH = 6
    }
}
