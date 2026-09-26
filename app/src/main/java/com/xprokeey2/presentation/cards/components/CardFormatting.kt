package com.xprokeey2.presentation.cards.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.xprokeey2.R
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory

private const val MASK = "••••"

/** Digit groups as printed on the card: 4-6-5 for Amex, otherwise blocks of 4. */
private fun groupSizes(brand: CardBrand): List<Int> =
    if (brand == CardBrand.AMEX) listOf(4, 6, 5) else List(5) { 4 }

/** "4111111111111111" → "4111 1111 1111 1111". */
fun formatCardNumber(digits: String, brand: CardBrand = CardBrand.detect(digits)): String {
    val groups = mutableListOf<String>()
    var rest = digits
    for (size in groupSizes(brand)) {
        if (rest.isEmpty()) break
        groups += rest.take(size)
        rest = rest.drop(size)
    }
    if (rest.isNotEmpty()) groups += rest
    return groups.joinToString(" ")
}

/** "•••• •••• •••• 2086", like the web list. */
fun maskedCardNumber(last4: String): String = "$MASK $MASK $MASK ${last4.ifEmpty { MASK }}"

/** Live preview while typing: the digits so far, then dots up to a full 16-digit card. */
fun previewCardNumber(digits: String): String {
    val brand = CardBrand.detect(digits)
    val length = if (brand == CardBrand.AMEX) 15 else maxOf(16, digits.length)
    return formatCardNumber(digits.padEnd(length, '•'), brand)
}

/** 7, 2028 → "07/28". */
fun formatExpiry(month: Int, year: Int): String =
    if (month in 1..12 && year > 0) "%02d/%02d".format(month, year % 100) else ""

/** Form digits "0728" → "07/28" (partial input stays partial). */
fun formatExpiryDigits(digits: String): String =
    if (digits.length <= 2) digits else digits.take(2) + "/" + digits.drop(2)

val CardCategory.labelRes: Int
    get() = when (this) {
        CardCategory.CREDIT -> R.string.category_credit
        CardCategory.DEBIT -> R.string.category_debit
        CardCategory.EMI -> R.string.category_emi
        CardCategory.PREPAID -> R.string.category_prepaid
        CardCategory.CORPORATE -> R.string.category_corporate
    }

/** Spaces the typed digits into card groups; the field's state keeps digits only. */
class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = formatCardNumber(digits)
        // Number of digits in front of each inserted space (the n-th space has n spaces before it).
        val groupEnds = formatted.indices.filter { formatted[it] == ' ' }.mapIndexed { n, index -> index - n }
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    offset + groupEnds.count { it < offset }

                override fun transformedToOriginal(offset: Int): Int =
                    (offset - formatted.take(offset).count { it == ' ' }).coerceIn(0, digits.length)
            },
        )
    }
}

/** Shows the form's "MMYY" digits as "MM / YY". */
class ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = if (digits.length <= 2) digits else digits.take(2) + " / " + digits.drop(2)
        return TransformedText(
            AnnotatedString(formatted),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = if (offset <= 2) offset else offset + 3
                override fun transformedToOriginal(offset: Int): Int =
                    (if (offset <= 2) offset else (offset - 3).coerceAtLeast(2)).coerceIn(0, digits.length)
            },
        )
    }
}
