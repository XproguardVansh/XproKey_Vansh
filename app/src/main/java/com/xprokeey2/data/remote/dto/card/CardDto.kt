package com.xprokeey2.data.remote.dto.card

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `data` of GET /cards/getallcard. */
@Serializable
data class CardListDto(
    val cards: List<CardDto> = emptyList(),
    val count: Int? = null,
)

/**
 * A card as returned by every card endpoint. [cardNumber] and [cvc] (vault-key ciphertext) are
 * only included by getcardbyid.
 */
@Serializable
data class CardDto(
    val id: Long,
    @SerialName("card_name") val cardName: String? = null,
    @SerialName("card_holder_name") val cardHolderName: String? = null,
    @SerialName("card_type") val cardType: String? = null,
    val brand: String? = null,
    val last4: String? = null,
    @SerialName("masked_card") val maskedCard: String? = null,
    @SerialName("bank_name") val bankName: String? = null,
    val notes: String? = null,
    @SerialName("expiry_month") val expiryMonth: Int? = null,
    @SerialName("expiry_year") val expiryYear: Int? = null,
    @SerialName("card_number") val cardNumber: String? = null,
    val cvc: String? = null,
)

/** Body of POST /cards/createcard and PUT /cards/updatecard/:id. */
@Serializable
data class CardRequestDto(
    @SerialName("card_name") val cardName: String,
    @SerialName("card_holder_name") val cardHolderName: String,
    @SerialName("card_type") val cardType: String,
    @SerialName("card_number") val cardNumber: String,
    val cvc: String,
    @SerialName("expiry_month") val expiryMonth: Int,
    @SerialName("expiry_year") val expiryYear: Int,
    val last4: String,
    val brand: String,
    @SerialName("bank_name") val bankName: String,
    val notes: String,
)
