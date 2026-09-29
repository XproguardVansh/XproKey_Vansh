package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

data class TicketFormErrors(
    val name: ValidationError? = null,
    val email: ValidationError? = null,
    val subject: ValidationError? = null,
    val message: ValidationError? = null,
) {
    val hasErrors: Boolean get() = name != null || email != null || subject != null || message != null
}

/**
 * The web ticket form: name, email, subject and details are required (after trimming), and the
 * email must look like one (the browser checks its `type="email"` field).
 */
class ValidateTicketFormUseCase @Inject constructor(
    private val validateEmail: ValidateEmailUseCase,
) {
    operator fun invoke(name: String, email: String, subject: String, message: String) = TicketFormErrors(
        name = if (name.isBlank()) ValidationError.REQUIRED else null,
        email = validateEmail(email),
        subject = if (subject.isBlank()) ValidationError.REQUIRED else null,
        message = if (message.isBlank()) ValidationError.REQUIRED else null,
    )
}
