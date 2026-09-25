package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

data class SignupFormErrors(
    val name: ValidationError? = null,
    val email: ValidationError? = null,
    val password: ValidationError? = null,
    val confirmPassword: ValidationError? = null,
    val terms: ValidationError? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, email, password, confirmPassword, terms).any { it != null }
}

class ValidateSignupFormUseCase @Inject constructor(
    private val validateEmail: ValidateEmailUseCase,
    private val validateNewPassword: ValidateNewPasswordUseCase,
) {
    operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        acceptedTerms: Boolean,
    ) = SignupFormErrors(
        name = if (name.isBlank()) ValidationError.REQUIRED else null,
        email = validateEmail(email),
        password = validateNewPassword(password),
        confirmPassword = when {
            confirmPassword.isEmpty() -> ValidationError.REQUIRED
            confirmPassword != password -> ValidationError.PASSWORD_MISMATCH
            else -> null
        },
        terms = if (acceptedTerms) null else ValidationError.TERMS_NOT_ACCEPTED,
    )
}
