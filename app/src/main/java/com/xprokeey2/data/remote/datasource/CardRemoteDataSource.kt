package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.card.CardDto
import com.xprokeey2.data.remote.dto.card.CardRequestDto
import com.xprokeey2.domain.util.Resource

interface CardRemoteDataSource {
    suspend fun getCards(): Resource<List<CardDto>>
    suspend fun getCard(id: Long): Resource<CardDto>
    suspend fun createCard(request: CardRequestDto): Resource<CardDto>
    suspend fun updateCard(id: Long, request: CardRequestDto): Resource<CardDto>

    /** Returns the server message. */
    suspend fun deleteCard(id: Long): Resource<String>
}
