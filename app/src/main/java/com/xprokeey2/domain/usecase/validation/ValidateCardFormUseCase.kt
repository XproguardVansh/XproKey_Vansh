package com.xprokeey2.domain.usecase.validation

import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.isValidCardNumber
import java.time.YearMonth
import javax.inject.Inject

enum class CardFormField { LABEL, NUMBER, EXPIRY, CVC }

/** The first problem found in the card form, and which field it belongs to. */
data class CardFormError(val field: CardFormField, val error: ValidationError)

/**
 * Card form checks in the web app's order (card validation spec §3), stopping at the first
 * failure. Android is a little stricter on expiry, as the spec recommends: the year must have two
 * digits and the card must not be expired yet (the server would reject it anyway).
 */
class ValidateCardFormUseCase @Inject constructor() {

    /**
     * [number], [expiry] ("MMYY") and [cvc] are the digits typed. On Edit, [checkNumber] is false
     * when the number wasn't changed (the web only runs Luhn on a changed number), and
     * [checkNotExpired] is false when the expiry wasn't changed (an old card can still be edited).
     */
    operator fun invoke(
        label: String,
        number: String,
        expiry: String,
        cvc: String,
        brand: CardBrand,
        checkNumber: Boolean = true,
        checkNotExpired: Boolean = true,
        today: YearMonth = YearMonth.now(),
    ): CardFormError? {
        if (label.isBlank()) return CardFormError(CardFormField.LABEL, ValidationError.CARD_LABEL_REQUIRED)
        if (number.isEmpty()) return CardFormError(CardFormField.NUMBER, ValidationError.CARD_NUMBER_REQUIRED)
        expiryError(expiry, today.takeIf { checkNotExpired })?.let { return CardFormError(CardFormField.EXPIRY, it) }
        if (cvc.isEmpty()) return CardFormError(CardFormField.CVC, ValidationError.CVC_REQUIRED)
        if (cvc.length != brand.cvcLength || !cvc.all(Char::isDigit)) {
            val error = if (brand.cvcLength == 4) ValidationError.CVC_MUST_BE_4_DIGITS else ValidationError.CVC_MUST_BE_3_DIGITS
            return CardFormError(CardFormField.CVC, error)
        }
        if (checkNumber && !isValidCardNumber(number)) {
            return CardFormError(CardFormField.NUMBER, ValidationError.INVALID_CARD_NUMBER)
        }
        return null
    }

    /** "MMYY" digits as typed; "1" is month 1 with no year yet, like the web's "MM / YY" parsing. */
    private fun expiryError(digits: String, today: YearMonth?): ValidationError? {
        val month = digits.take(2).toIntOrNull() ?: 0
        val yearText = digits.drop(2)
        val year = yearText.toIntOrNull() ?: 0
        return when {
            month !in 1..12 -> ValidationError.EXPIRY_MONTH_INVALID
            year == 0 -> ValidationError.EXPIRY_YEAR_REQUIRED
            yearText.length != 2 -> ValidationError.EXPIRY_YEAR_TWO_DIGITS
            today != null && YearMonth.of(2000 + year, month).isBefore(today) -> ValidationError.CARD_EXPIRED
            else -> null
        }
    }
}
