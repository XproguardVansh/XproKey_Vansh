package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

class ValidateEmailUseCase @Inject constructor() {

    /** Returns null when the email is valid. */
    operator fun invoke(email: String): ValidationError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.REQUIRED
            !EMAIL_REGEX.matches(trimmed) -> ValidationError.INVALID_EMAIL
            else -> null
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
