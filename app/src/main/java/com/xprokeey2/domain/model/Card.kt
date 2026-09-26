package com.xprokeey2.domain.model

/** A saved payment card as the server lists it: everything except the encrypted number and CVC. */
data class Card(
    val id: Long,
    /** "Card type" in the web form (`card_name`), e.g. "Personal". */
    val label: String,
    val holderName: String,
    /** Raw `card_type`, e.g. "credit"; see [CardCategory]. */
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

/** What the user entered in the Add / Edit card form. [number] and [cvc] are digits only. */
data class CardDraft(
    val label: String,
    val holderName: String,
    val number: String,
    val cvc: String,
    val expiry: CardExpiry,
    val category: CardCategory,
    val bankName: String,
    val notes: String,
)

/** Body of createcard / updatecard, with the number and CVC already encrypted client-side. */
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

/** `card_type` values; the web sends "credit" and "debit". */
enum class CardCategory(val apiValue: String) {
    CREDIT("credit"),
    DEBIT("debit"),
    EMI("emi"),
    PREPAID("prepaid"),
    CORPORATE("corporate");

    companion object {
        fun fromApiValue(value: String?): CardCategory? =
            entries.firstOrNull { it.apiValue.equals(value?.trim(), ignoreCase = true) }
    }
}

/** Card network. [apiValue] is sent as `brand`; the web sends upper case (e.g. "RUPAY"). */
enum class CardBrand(val apiValue: String, val displayName: String) {
    VISA("VISA", "Visa"),
    MASTERCARD("MASTERCARD", "Mastercard"),
    AMEX("AMEX", "American Express"),
    RUPAY("RUPAY", "RuPay"),
    DISCOVER("DISCOVER", "Discover"),
    DINERS("DINERS", "Diners Club"),
    JCB("JCB", "JCB"),
    MAESTRO("MAESTRO", "Maestro"),
    UNKNOWN("", "");

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

        /** Detects the network from the number's leading digits (issuer ranges). */
        fun detect(number: String): CardBrand {
            val digits = number.filter(Char::isDigit)
            fun startsIn(length: Int, range: IntRange): Boolean =
                digits.length >= length && digits.take(length).toInt() in range

            return when {
                digits.startsWith("4") -> VISA
                startsIn(2, 34..34) || startsIn(2, 37..37) -> AMEX
                startsIn(2, 51..55) || startsIn(4, 2221..2720) -> MASTERCARD
                startsIn(6, 652150..653149) -> RUPAY
                digits.startsWith("6011") || startsIn(3, 644..649) || digits.startsWith("65") -> DISCOVER
                digits.startsWith("508") || digits.startsWith("60") ||
                    digits.startsWith("81") || digits.startsWith("82") -> RUPAY
                startsIn(4, 3528..3589) -> JCB
                startsIn(3, 300..305) || digits.startsWith("36") || digits.startsWith("38") ||
                    digits.startsWith("39") -> DINERS
                digits.startsWith("50") || startsIn(2, 56..58) || digits.startsWith("6304") ||
                    digits.startsWith("6759") || startsIn(4, 6761..6763) -> MAESTRO
                else -> UNKNOWN
            }
        }
    }
}
