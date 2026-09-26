package com.xprokeey2.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTest {

    @Test
    fun detectsNetworkFromNumber() {
        assertEquals(CardBrand.VISA, CardBrand.detect("4111111111111111"))
        assertEquals(CardBrand.MASTERCARD, CardBrand.detect("5454545454544421"))
        assertEquals(CardBrand.MASTERCARD, CardBrand.detect("2221000000000009"))
        assertEquals(CardBrand.AMEX, CardBrand.detect("378282246310005"))
        // RuPay test card from the Postman collection, and the other RuPay ranges.
        assertEquals(CardBrand.RUPAY, CardBrand.detect("65228143420114"))
        assertEquals(CardBrand.RUPAY, CardBrand.detect("6071234567890123"))
        assertEquals(CardBrand.RUPAY, CardBrand.detect("5085123456789012"))
        assertEquals(CardBrand.DISCOVER, CardBrand.detect("6011111111111117"))
        assertEquals(CardBrand.UNKNOWN, CardBrand.detect(""))
    }

    @Test
    fun readsBrandInAnyCase() {
        assertEquals(CardBrand.RUPAY, CardBrand.fromApiValue("RUPAY")) // web app
        assertEquals(CardBrand.VISA, CardBrand.fromApiValue("visa")) // Postman
        assertEquals(CardBrand.AMEX, CardBrand.fromApiValue("American Express"))
        assertEquals(CardBrand.UNKNOWN, CardBrand.fromApiValue(null))
        assertEquals(CardBrand.UNKNOWN, CardBrand.fromApiValue("something-else"))
    }

    @Test
    fun parsesFormExpiry() {
        assertEquals(CardExpiry(month = 7, year = 2028), CardExpiry.parse("0728"))
        assertEquals(CardExpiry(month = 12, year = 2030), CardExpiry.parse("1230"))
        assertNull(CardExpiry.parse("1328"))
        assertNull(CardExpiry.parse("0028"))
        assertNull(CardExpiry.parse("072"))
    }

    @Test
    fun categoryMatchesApiValues() {
        assertEquals(CardCategory.DEBIT, CardCategory.fromApiValue("debit"))
        assertEquals(CardCategory.CREDIT, CardCategory.fromApiValue("Credit"))
        assertNull(CardCategory.fromApiValue(""))
    }
}
