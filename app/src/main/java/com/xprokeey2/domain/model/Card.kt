package com.xprokeey2.domain.model

/** A saved payment card as the server lists it: everything except the encrypted number and CVC. */
data class Card(
    val id: Long,
    /** "Card type" in the web form (`card_name`), e.g. "Personal". */
    val label: String,
    val holderName: String,
    /** Raw `card_type`: "credit" or "debit"; see [CardCategory]. */
    val category: String,
    val brand: CardBrand,
    val last4: String,
    val bankName: String,
    val notes: String,
    val expiryMonth: Int,
    val expiryYear: Int,
)

/** [Card] as stored: [encryptedNumber] and [encryptedCvc] are ciphertext under the vault key. */
data class StoredCard(
    val card: Card,
    val encryptedNumber: String,
    val encryptedCvc: String,
)

/**
 * [Card] with its number and CVC decrypted. A value is null when it isn't ciphertext under this
 * vault key (e.g. cards saved in plain text from Postman), and "" when nothing was stored.
 */
data class CardDetails(
    val card: Card,
    val number: String?,
    val cvc: String?,
)

/**
 * A new card from the Add card form. [number] and [cvc] are digits only; [brand] is the one shown
 * in the form (detected from the number, or the last detected one when the number is unknown).
 */
data class CardDraft(
    val label: String,
    val holderName: String,
    val number: String,
    val cvc: String,
    val expiry: CardExpiry,
    val category: CardCategory,
    val brand: CardBrand,
    val bankName: String,
    val notes: String,
)

/**
 * Edit card: only the fields the user changed (null = unchanged, not sent). [number] and [cvc]
 * are plain digits; [brand] goes with a changed [number].
 */
data class CardChanges(
    val label: String? = null,
    val holderName: String? = null,
    val number: String? = null,
    val brand: CardBrand? = null,
    val cvc: String? = null,
    val expiry: CardExpiry? = null,
    val category: CardCategory? = null,
    val bankName: String? = null,
    val notes: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(label, holderName, number, cvc, expiry, category, bankName, notes).all { it == null }
}

/** Body of createcard, with the number and CVC already encrypted client-side. */
data class CardPayload(
    val label: String,
    val holderName: String,
    val category: CardCategory,
    val encryptedNumber: String,
    val encryptedCvc: String,
    val last4: String,
    val brand: CardBrand,
    val expiry: CardExpiry,
    val bankName: String,
    val notes: String,
)

/** Body of updatecard: only changed fields (null = not sent), secrets already encrypted. */
data class CardUpdatePayload(
    val label: String? = null,
    val holderName: String? = null,
    val category: CardCategory? = null,
    val encryptedNumber: String? = null,
    val encryptedCvc: String? = null,
    val last4: String? = null,
    val brand: CardBrand? = null,
    val expiry: CardExpiry? = null,
    val bankName: String? = null,
    val notes: String? = null,
)

data class CardExpiry(val month: Int, val year: Int) {
    companion object {
        /** Parses the form's "MMYY" digits, e.g. "0728" → 07/2028. Null unless the month is 01-12. */
        fun parse(digits: String): CardExpiry? {
            if (digits.length != 4 || !digits.all(Char::isDigit)) return null
            val month = digits.take(2).toInt()
            if (month !in 1..12) return null
            return CardExpiry(month = month, year = 2000 + digits.takeLast(2).toInt())
        }
    }
}

/**
 * The web form's "Card category". The server only knows `card_type` "credit" / "debit", so
 * Debit and Prepaid are saved as debit and the rest as credit (card validation spec §7.3).
 */
enum class CardCategory(val cardType: String) {
    CREDIT(CARD_TYPE_CREDIT),
    DEBIT(CARD_TYPE_DEBIT),
    EMI(CARD_TYPE_CREDIT),
    PREPAID(CARD_TYPE_DEBIT),
    CORPORATE(CARD_TYPE_CREDIT);

    companion object {
        /** What a stored `card_type` reads back as: Debit card or Credit card. */
        fun fromCardType(value: String?): CardCategory? = when (value?.trim()?.lowercase()) {
            CARD_TYPE_CREDIT -> CREDIT
            CARD_TYPE_DEBIT -> DEBIT
            else -> null
        }
    }
}

private const val CARD_TYPE_CREDIT = "credit"
private const val CARD_TYPE_DEBIT = "debit"

/** Card network, sent as `brand` in upper case like the web app. */
enum class CardBrand(val apiValue: String, val displayName: String) {
    AMEX("AMEX", "American Express"),
    VISA("VISA", "Visa"),
    MASTERCARD("MASTERCARD", "Mastercard"),
    JCB("JCB", "JCB"),
    DINERS("DINERS", "Diners Club"),
    RUPAY("RUPAY", "RuPay"),
    DISCOVER("DISCOVER", "Discover"),
    UNIONPAY("UNIONPAY", "UnionPay"),
    UNKNOWN("", "");

    /** CVC length: 4 for Amex, 3 for every other network (spec §7.2). */
    val cvcLength: Int get() = if (this == AMEX) 4 else 3

    companion object {
        /** Case-insensitive: the web stores "RUPAY", Postman test cards "visa". */
        fun fromApiValue(value: String?): CardBrand {
            val key = value?.uppercase()?.filter(Char::isLetter).orEmpty()
            return when (key) {
                "" -> UNKNOWN
                "AMERICANEXPRESS" -> AMEX
                "DINERSCLUB" -> DINERS
                else -> entries.firstOrNull { it.apiValue == key } ?: UNKNOWN
            }
        }

        private val MASTERCARD_2_SERIES = Regex("^2(22[1-9]|2[3-9]\\d|[3-6]\\d{2}|7[01]\\d|720)")
        private val JCB_RANGE = Regex("^35(2[89]|[3-8]\\d)")

        /**
         * Same rules, in the same order, as the web app (card validation spec §6): the first match
         * wins, so do not reorder.
         */
        fun detect(number: String): CardBrand {
            val n = number.filter(Char::isDigit)
            return when {
                n.isEmpty() -> UNKNOWN
                n.startsWith("34") || n.startsWith("37") -> AMEX
                n.startsWith("4") -> VISA
                n.length >= 2 && n.take(2).toInt() in 51..55 -> MASTERCARD
                MASTERCARD_2_SERIES.containsMatchIn(n) -> MASTERCARD
                JCB_RANGE.containsMatchIn(n) -> JCB
                n.length >= 3 && n.take(3).toInt() in 300..305 -> DINERS
                n.startsWith("36") || n.startsWith("38") || n.startsWith("39") -> DINERS
                // RuPay 6521/6522 must come before Discover 65.
                n.startsWith("6521") || n.startsWith("6522") -> RUPAY
                n.startsWith("6011") -> DISCOVER
                n.length >= 3 && n.take(3).toInt() in 644..649 -> DISCOVER
                n.startsWith("65") -> DISCOVER
                n.length >= 6 && n.take(6) in "622126".."622925" -> DISCOVER
                // UnionPay only after the Discover range above.
                n.startsWith("62") -> UNIONPAY
                // RuPay 60/81/82 only after Discover 6011.
                n.startsWith("60") || n.startsWith("81") || n.startsWith("82") -> RUPAY
                else -> UNKNOWN
            }
        }

        /**
         * Web behaviour (spec §6.1): an unrecognised number keeps the previously shown brand,
         * which starts as Visa, so a card is never saved without a brand.
         */
        fun detectOrKeep(number: String, previous: CardBrand): CardBrand =
            detect(number).takeIf { it != UNKNOWN } ?: previous.takeIf { it != UNKNOWN } ?: VISA
    }
}

/** Luhn checksum and 13-16 digits, as the web app checks card numbers (spec §5.2). */
fun isValidCardNumber(number: String): Boolean {
    val digits = number.filter(Char::isDigit)
    if (digits.length !in 13..16) return false
    var sum = 0
    var double = false
    for (i in digits.indices.reversed()) {
        var n = digits[i] - '0'
        if (double) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        double = !double
    }
    return sum % 10 == 0
}
