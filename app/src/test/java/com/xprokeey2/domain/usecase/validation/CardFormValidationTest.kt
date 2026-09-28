package com.xprokeey2.domain.usecase.validation

import com.xprokeey2.domain.model.CardBrand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

/** Cases from the card validation spec §10, with today = September 2026. */
class CardFormValidationTest {

    private val validate = ValidateCardFormUseCase()
    private val today = YearMonth.of(2026, 9)

    private fun check(
        label: String = "Personal",
        number: String = "4111111111111111",
        expiry: String = "1229",
        cvc: String = "123",
        brand: CardBrand = CardBrand.VISA,
        checkNumber: Boolean = true,
        checkNotExpired: Boolean = true,
    ) = validate(label, number, expiry, cvc, brand, checkNumber, checkNotExpired, today)

    private fun assertError(field: CardFormField, error: ValidationError, actual: CardFormError?) =
        assertEquals(CardFormError(field, error), actual)

    @Test
    fun validCardPasses() {
        assertNull(check())
        assertNull(check(expiry = "0926")) // current month is still valid
        assertNull(check(number = "378282246310005", cvc = "1234", brand = CardBrand.AMEX))
    }

    @Test
    fun checksRunInWebOrderAndStopAtTheFirst() {
        assertError(CardFormField.LABEL, ValidationError.CARD_LABEL_REQUIRED, check(label = " ", number = ""))
        assertError(CardFormField.NUMBER, ValidationError.CARD_NUMBER_REQUIRED, check(number = "", cvc = ""))
        // Luhn runs last: an empty CVC is reported before a bad number.
        assertError(CardFormField.CVC, ValidationError.CVC_REQUIRED, check(number = "4111111111111112", cvc = ""))
        assertError(CardFormField.NUMBER, ValidationError.INVALID_CARD_NUMBER, check(number = "4111111111111112"))
        assertError(CardFormField.NUMBER, ValidationError.INVALID_CARD_NUMBER, check(number = "411111111111"))
    }

    @Test
    fun expiryRules() {
        assertError(CardFormField.EXPIRY, ValidationError.EXPIRY_MONTH_INVALID, check(expiry = "1330"))
        assertError(CardFormField.EXPIRY, ValidationError.EXPIRY_MONTH_INVALID, check(expiry = "0030"))
        assertError(CardFormField.EXPIRY, ValidationError.EXPIRY_MONTH_INVALID, check(expiry = ""))
        assertError(CardFormField.EXPIRY, ValidationError.EXPIRY_YEAR_REQUIRED, check(expiry = "12"))
        assertError(CardFormField.EXPIRY, ValidationError.EXPIRY_YEAR_TWO_DIGITS, check(expiry = "122"))
        assertError(CardFormField.EXPIRY, ValidationError.CARD_EXPIRED, check(expiry = "0826"))
    }

    @Test
    fun cvcLengthDependsOnBrand() {
        assertError(CardFormField.CVC, ValidationError.CVC_MUST_BE_3_DIGITS, check(cvc = "12"))
        assertError(
            CardFormField.CVC,
            ValidationError.CVC_MUST_BE_4_DIGITS,
            check(number = "378282246310005", cvc = "123", brand = CardBrand.AMEX),
        )
    }

    @Test
    fun editSkipsChecksForUnchangedNumberAndExpiry() {
        assertNull(check(number = "4111111111111112", checkNumber = false))
        assertNull(check(expiry = "0826", checkNotExpired = false))
    }
}
