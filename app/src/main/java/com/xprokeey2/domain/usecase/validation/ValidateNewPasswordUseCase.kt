package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

/** Rule for any new master password (signup and password reset). */
class ValidateNewPasswordUseCase @Inject constructor() {

    /** Returns null when the password is acceptable. */
    operator fun invoke(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.REQUIRED
        password.length < MIN_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        else -> null
    }

    companion object {
        const val MIN_LENGTH = 8
    }
}
