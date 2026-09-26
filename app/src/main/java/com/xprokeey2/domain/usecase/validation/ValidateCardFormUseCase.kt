package com.xprokeey2.domain.usecase.validation

import com.xprokeey2.domain.model.CardExpiry
import javax.inject.Inject

data class CardFormErrors(
    val label: ValidationError? = null,
    val number: ValidationError? = null,
    val expiry: ValidationError? = null,
    val cvc: ValidationError? = null,
) {
    val hasErrors: Boolean get() = label != null || number != null || expiry != null || cvc != null
}

/**
 * Required fields match the web form (card type, number, expiry, CVV). [number], [expiry] ("MMYY")
 * and [cvc] are the digits the user typed.
 */
class ValidateCardFormUseCase @Inject constructor() {

    operator fun invoke(label: String, number: String, expiry: String, cvc: String) = CardFormErrors(
        label = if (label.isBlank()) ValidationError.REQUIRED else null,
        number = when {
            number.isEmpty() -> ValidationError.REQUIRED
            !number.all(Char::isDigit) || number.length !in CARD_NUMBER_LENGTH -> ValidationError.INVALID_CARD_NUMBER
            else -> null
        },
        expiry = when {
            expiry.isEmpty() -> ValidationError.REQUIRED
            CardExpiry.parse(expiry) == null -> ValidationError.INVALID_EXPIRY
            else -> null
        },
        cvc = when {
            cvc.isEmpty() -> ValidationError.REQUIRED
            !cvc.all(Char::isDigit) || cvc.length !in CVC_LENGTH -> ValidationError.INVALID_CVC
            else -> null
        },
    )

    private companion object {
        val CARD_NUMBER_LENGTH = 12..19
        val CVC_LENGTH = 3..4
    }
}
