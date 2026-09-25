package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

data class LoginFormErrors(
    val email: ValidationError? = null,
    val password: ValidationError? = null,
) {
    val hasErrors: Boolean get() = email != null || password != null
}

class ValidateLoginFormUseCase @Inject constructor(
    private val validateEmail: ValidateEmailUseCase,
) {
    operator fun invoke(email: String, password: String) = LoginFormErrors(
        email = validateEmail(email),
        password = if (password.isEmpty()) ValidationError.REQUIRED else null,
    )
}
