package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.model.StoredCard
import com.xprokeey2.domain.util.Resource

interface CardRepository {

    suspend fun getCards(): Resource<List<Card>>

    /** One card including its encrypted number and CVC. */
    suspend fun getCard(id: Long): Resource<StoredCard>

    suspend fun createCard(payload: CardPayload): Resource<Card>

    suspend fun updateCard(id: Long, payload: CardPayload): Resource<Card>

    /** Returns the server message. */
    suspend fun deleteCard(id: Long): Resource<String>
}
