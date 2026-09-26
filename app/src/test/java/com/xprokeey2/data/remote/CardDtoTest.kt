package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.mapper.toStoredCard
import com.xprokeey2.data.remote.dto.card.CardDto
import com.xprokeey2.data.remote.dto.card.CardListDto
import com.xprokeey2.data.remote.dto.card.CardRequestDto
import com.xprokeey2.data.remote.dto.common.DataResponseDto
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardExpiry
import com.xprokeey2.domain.model.CardPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Real responses from the card endpoints, as captured in the web app. */
class CardDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesCardList() {
        val body = """
            {"data": {"cards": [
              {"bank_name": "BOB", "brand": "RUPAY", "card_holder_name": "VANSH GOEL", "card_name": "Personal",
               "card_type": "debit", "created_at": "2026-09-26T07:26:57.588329Z", "expiry_month": 7,
               "expiry_year": 2028, "id": 267, "last4": "2086", "masked_card": "**** **** **** 2086",
               "notes": "", "updated_at": "2026-09-26T07:26:57.588329Z"}
            ], "count": 1}, "message": "Cards retrieved successfully"}
        """.trimIndent()

        val card = json.decodeFromString<DataResponseDto<CardListDto>>(body).data.cards.single().toDomain()

        assertEquals(267L, card.id)
        assertEquals("Personal", card.label)
        assertEquals("VANSH GOEL", card.holderName)
        assertEquals("debit", card.category)
        assertEquals(CardBrand.RUPAY, card.brand)
        assertEquals("2086", card.last4)
        assertEquals("BOB", card.bankName)
        assertEquals(7, card.expiryMonth)
        assertEquals(2028, card.expiryYear)
    }

    @Test
    fun cardByIdCarriesTheSecrets() {
        val body = """
            {"data": {"bank_name": "", "brand": "visa", "card_holder_name": "Shreyash Jadhav", "card_name": "Visa Test",
             "card_number": "4111111111111111", "card_type": "credit", "cvc": "1234", "expiry_month": 12,
             "expiry_year": 2030, "id": 268, "last4": "1111", "masked_card": "**** **** **** 1111",
             "notes": "Visa test card"}, "message": "Card retrieved successfully"}
        """.trimIndent()

        val stored = json.decodeFromString<DataResponseDto<CardDto>>(body).data.toStoredCard()

        assertEquals("4111111111111111", stored.encryptedNumber)
        assertEquals("1234", stored.encryptedCvc)
        assertEquals(CardBrand.VISA, stored.card.brand)
    }

    @Test
    fun requestUsesTheServerFieldNames() {
        val payload = CardPayload(
            label = "Personal",
            holderName = "Vansh Goel",
            category = CardCategory.DEBIT,
            encryptedNumber = "cipher-number",
            encryptedCvc = "cipher-cvc",
            last4 = "2086",
            brand = CardBrand.RUPAY,
            expiry = CardExpiry(7, 2028),
            bankName = "BOB",
            notes = "",
        )

        val body = json.encodeToJsonElement(CardRequestDto.serializer(), payload.toRequestDto())
            .jsonObject

        assertEquals(
            setOf(
                "card_name", "card_holder_name", "card_type", "card_number", "cvc",
                "expiry_month", "expiry_year", "last4", "brand", "bank_name", "notes",
            ),
            body.keys,
        )
        assertEquals("\"debit\"", body["card_type"].toString())
        assertEquals("\"RUPAY\"", body["brand"].toString())
        assertEquals("2028", body["expiry_year"].toString())
    }
}
