package com.xprokeey2.domain.usecase.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TicketFormValidationTest {

    private val validate = ValidateTicketFormUseCase(ValidateEmailUseCase())

    @Test
    fun filledFormPasses() {
        assertFalse(validate("Vansh", "goelv2610@gmail.com", "Testing", "Testing").hasErrors)
    }

    @Test
    fun everyFieldIsRequiredAfterTrimming() {
        val errors = validate(" ", "", "  ", "\n")
        assertEquals(ValidationError.REQUIRED, errors.name)
        assertEquals(ValidationError.REQUIRED, errors.email)
        assertEquals(ValidationError.REQUIRED, errors.subject)
        assertEquals(ValidationError.REQUIRED, errors.message)
    }

    @Test
    fun emailMustLookLikeOne() {
        assertEquals(ValidationError.INVALID_EMAIL, validate("Vansh", "goelv2610", "s", "m").email)
        assertNull(validate("Vansh", " goelv2610@gmail.com ", "s", "m").email)
    }
}
