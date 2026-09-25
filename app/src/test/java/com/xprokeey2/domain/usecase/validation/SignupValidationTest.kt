package com.xprokeey2.domain.usecase.validation

import com.xprokeey2.domain.model.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupValidationTest {

    private val evaluate = EvaluatePasswordStrengthUseCase()
    private val validate = ValidateSignupFormUseCase(ValidateEmailUseCase(), ValidateNewPasswordUseCase())
    private val validateReset = ValidateResetPasswordFormUseCase(ValidateOtpUseCase(), ValidateNewPasswordUseCase())

    @Test
    fun strengthMatchesWebScoring() {
        assertEquals(0, evaluate("abc").score)
        assertEquals(PasswordStrength.Level.WEAK, evaluate("abcdef").level) // length only
        assertEquals(PasswordStrength.Level.OKAY, evaluate("abcde1").level) // + digit
        assertEquals(PasswordStrength.Level.GOOD, evaluate("abcde1!").level) // + special
        assertEquals(PasswordStrength.Level.STRONG, evaluate("Abcde1!").level) // + uppercase
        assertEquals(2, evaluate("A1").score) // short passwords can still earn points
    }

    @Test
    fun validFormHasNoErrors() {
        assertFalse(validate("Aarav", "a@b.co", "12345678", "12345678", acceptedTerms = true).hasErrors)
    }

    @Test
    fun passwordShorterThanEightIsRejected() {
        val errors = validate("Aarav", "a@b.co", "1234567", "1234567", acceptedTerms = true)
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, errors.password)
    }

    @Test
    fun termsMustBeAccepted() {
        val errors = validate("Aarav", "a@b.co", "12345678", "12345678", acceptedTerms = false)
        assertEquals(ValidationError.TERMS_NOT_ACCEPTED, errors.terms)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun mismatchedConfirmationIsRejected() {
        val errors = validate("Aarav", "a@b.co", "12345678", "12345679", acceptedTerms = true)
        assertEquals(ValidationError.PASSWORD_MISMATCH, errors.confirmPassword)
    }

    @Test
    fun resetFormNeedsSixDigitOtpRecoveryKeyAndEightCharPassword() {
        val errors = validateReset("12", "", "short", "short", recoveryKeyRequired = true)
        assertEquals(ValidationError.INVALID_OTP, errors.otp)
        assertEquals(ValidationError.REQUIRED, errors.recoveryKey)
        assertEquals(ValidationError.PASSWORD_TOO_SHORT, errors.newPassword)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun resetFormSkipsRecoveryKeyForAccountsWithoutVault() {
        val errors = validateReset("034862", "", "Vansh@Vansh", "Vansh@Vansh", recoveryKeyRequired = false)
        assertFalse(errors.hasErrors)
    }
}
