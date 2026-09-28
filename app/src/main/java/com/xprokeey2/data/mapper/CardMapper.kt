package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.card.CardDto
import com.xprokeey2.data.remote.dto.card.CardRequestDto
import com.xprokeey2.data.remote.dto.card.CardUpdateRequestDto
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.model.CardUpdatePayload
import com.xprokeey2.domain.model.StoredCard

fun CardDto.toDomain() = Card(
    id = id,
    label = cardName.orEmpty(),
    holderName = cardHolderName.orEmpty(),
    category = cardType.orEmpty(),
    brand = CardBrand.fromApiValue(brand),
    last4 = last4?.takeIf { it.isNotBlank() } ?: maskedCard.orEmpty().filter(Char::isDigit).takeLast(4),
    bankName = bankName.orEmpty(),
    notes = notes.orEmpty(),
    expiryMonth = expiryMonth ?: 0,
    expiryYear = expiryYear ?: 0,
)

fun CardDto.toStoredCard() = StoredCard(
    card = toDomain(),
    encryptedNumber = cardNumber.orEmpty(),
    encryptedCvc = cvc.orEmpty(),
)

fun CardPayload.toRequestDto() = CardRequestDto(
    cardName = label,
    cardHolderName = holderName,
    cardType = category.cardType,
    cardNumber = encryptedNumber,
    cvc = encryptedCvc,
    expiryMonth = expiry.month,
    expiryYear = expiry.year,
    last4 = last4,
    brand = brand.apiValue,
    bankName = bankName,
    notes = notes,
)

/** Month and year always travel together: the server only checks "not expired" when both are sent. */
fun CardUpdatePayload.toRequestDto() = CardUpdateRequestDto(
    cardName = label,
    cardHolderName = holderName,
    cardType = category?.cardType,
    cardNumber = encryptedNumber,
    cvc = encryptedCvc,
    expiryMonth = expiry?.month,
    expiryYear = expiry?.year,
    last4 = last4,
    brand = brand?.apiValue,
    bankName = bankName,
    notes = notes,
)
