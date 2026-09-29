package com.xprokeey2.domain.usecase.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The web Change password checks: one message at a time, in the page's order. */
class ChangePasswordFormValidationTest {

    private val validate = ValidateChangePasswordFormUseCase()

    @Test
    fun otpIsCheckedFirst() {
        assertEquals(ChangePasswordFormError.OTP_REQUIRED, validate(otp = "", newPassword = "", confirmPassword = "x"))
        assertEquals(ChangePasswordFormError.OTP_REQUIRED, validate(otp = "   ", newPassword = "Password1", confirmPassword = "Password1"))
        assertEquals(ChangePasswordFormError.OTP_WRONG_LENGTH, validate(otp = "12345", newPassword = "", confirmPassword = ""))
    }

    @Test
    fun thenTheNewPasswordAndItsConfirmation() {
        assertEquals(ChangePasswordFormError.PASSWORD_REQUIRED, validate(otp = " 123456 ", newPassword = "", confirmPassword = ""))
        assertEquals(ChangePasswordFormError.PASSWORD_TOO_SHORT, validate(otp = "123456", newPassword = "Pass123", confirmPassword = "Pass123"))
        assertEquals(ChangePasswordFormError.PASSWORD_MISMATCH, validate(otp = "123456", newPassword = "Password1", confirmPassword = "Password2"))
    }

    @Test
    fun validFormPasses() {
        assertNull(validate(otp = "123456", newPassword = "Pass1234", confirmPassword = "Pass1234"))
    }
}
