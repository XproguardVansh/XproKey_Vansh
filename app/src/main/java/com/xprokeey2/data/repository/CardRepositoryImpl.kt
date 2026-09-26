package com.xprokeey2.data.repository

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.mapper.toStoredCard
import com.xprokeey2.data.remote.datasource.CardRemoteDataSource
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardPayload
import com.xprokeey2.domain.model.StoredCard
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val remote: CardRemoteDataSource,
) : CardRepository {

    override suspend fun getCards(): Resource<List<Card>> =
        remote.getCards().map { cards -> cards.map { it.toDomain() } }

    override suspend fun getCard(id: Long): Resource<StoredCard> =
        remote.getCard(id).map { it.toStoredCard() }

    override suspend fun createCard(payload: CardPayload): Resource<Card> =
        remote.createCard(payload.toRequestDto()).map { it.toDomain() }

    override suspend fun updateCard(id: Long, payload: CardPayload): Resource<Card> =
        remote.updateCard(id, payload.toRequestDto()).map { it.toDomain() }

    override suspend fun deleteCard(id: Long): Resource<String> = remote.deleteCard(id)
}
