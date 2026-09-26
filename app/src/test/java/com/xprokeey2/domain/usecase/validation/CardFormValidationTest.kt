package com.xprokeey2.domain.usecase.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CardFormValidationTest {

    private val validate = ValidateCardFormUseCase()

    @Test
    fun validCardHasNoErrors() {
        assertFalse(validate(label = "Personal", number = "4111111111111111", expiry = "1230", cvc = "123").hasErrors)
    }

    @Test
    fun requiredFieldsMatchTheWebForm() {
        val errors = validate(label = " ", number = "", expiry = "", cvc = "")
        assertEquals(ValidationError.REQUIRED, errors.label)
        assertEquals(ValidationError.REQUIRED, errors.number)
        assertEquals(ValidationError.REQUIRED, errors.expiry)
        assertEquals(ValidationError.REQUIRED, errors.cvc)
    }

    @Test
    fun rejectsMalformedValues() {
        val errors = validate(label = "Personal", number = "41111", expiry = "1330", cvc = "12")
        assertEquals(ValidationError.INVALID_CARD_NUMBER, errors.number)
        assertEquals(ValidationError.INVALID_EXPIRY, errors.expiry)
        assertEquals(ValidationError.INVALID_CVC, errors.cvc)
        assertNull(errors.label)
    }
}
