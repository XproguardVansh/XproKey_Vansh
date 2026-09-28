package com.xprokeey2.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cases from the card validation spec §10. */
class CardTest {

    @Test
    fun detectsBrandLikeTheWebApp() {
        val cases = mapOf(
            "4111 1111 1111 1111" to CardBrand.VISA,
            "5555 5555 5555 4444" to CardBrand.MASTERCARD,
            "2221 0000 0000 0009" to CardBrand.MASTERCARD,
            "3782 822463 10005" to CardBrand.AMEX,
            "3530 1113 3330 0000" to CardBrand.JCB,
            "3056 930902 5904" to CardBrand.DINERS,
            "6011 1111 1111 1117" to CardBrand.DISCOVER,
            "6440 0000 0000 0005" to CardBrand.DISCOVER,
            "6221 2600 0000 0000" to CardBrand.DISCOVER,
            "6200 0000 0000 0005" to CardBrand.UNIONPAY,
            "6521 0000 0000 0007" to CardBrand.RUPAY,
            "6080 0000 0000 0000" to CardBrand.RUPAY,
            "8100 0000 0000 0002" to CardBrand.RUPAY,
            "9999 0000 0000 0000" to CardBrand.UNKNOWN,
            "" to CardBrand.UNKNOWN,
        )
        cases.forEach { (number, brand) -> assertEquals(number, brand, CardBrand.detect(number)) }
    }

    @Test
    fun unknownNumberKeepsThePreviousBrandStartingWithVisa() {
        assertEquals(CardBrand.VISA, CardBrand.detectOrKeep("9999", previous = CardBrand.VISA))
        assertEquals(CardBrand.RUPAY, CardBrand.detectOrKeep("9999", previous = CardBrand.RUPAY))
        assertEquals(CardBrand.AMEX, CardBrand.detectOrKeep("37", previous = CardBrand.VISA))
    }

    @Test
    fun validatesNumbersLikeTheWebApp() {
        assertTrue(isValidCardNumber("4111 1111 1111 1111"))
        assertTrue(isValidCardNumber("4111-1111-1111-1111"))
        assertTrue(isValidCardNumber("4640464646484465"))
        assertFalse(isValidCardNumber("4111 1111 1111 1112")) // fails Luhn
        assertFalse(isValidCardNumber("4111 1111 1111")) // 12 digits
        assertFalse(isValidCardNumber("41111111111111111")) // 17 digits
    }

    @Test
    fun readsBrandInAnyCase() {
        assertEquals(CardBrand.RUPAY, CardBrand.fromApiValue("RUPAY")) // web app
        assertEquals(CardBrand.VISA, CardBrand.fromApiValue("visa")) // Postman
        assertEquals(CardBrand.UNIONPAY, CardBrand.fromApiValue("UNIONPAY"))
        assertEquals(CardBrand.UNKNOWN, CardBrand.fromApiValue(null))
    }

    @Test
    fun cvcLengthIsFourOnlyForAmex() {
        assertEquals(4, CardBrand.AMEX.cvcLength)
        assertEquals(3, CardBrand.VISA.cvcLength)
        assertEquals(3, CardBrand.DINERS.cvcLength)
    }

    @Test
    fun categoryMapsToCreditOrDebit() {
        assertEquals("credit", CardCategory.CREDIT.cardType)
        assertEquals("debit", CardCategory.DEBIT.cardType)
        assertEquals("credit", CardCategory.EMI.cardType)
        assertEquals("debit", CardCategory.PREPAID.cardType)
        assertEquals("credit", CardCategory.CORPORATE.cardType)
        assertEquals(CardCategory.DEBIT, CardCategory.fromCardType("debit"))
        assertEquals(CardCategory.CREDIT, CardCategory.fromCardType("Credit"))
        assertNull(CardCategory.fromCardType("corporate"))
    }

    @Test
    fun parsesFormExpiry() {
        assertEquals(CardExpiry(month = 12, year = 2029), CardExpiry.parse("1229"))
        assertNull(CardExpiry.parse("1330"))
        assertNull(CardExpiry.parse("122"))
    }
}
