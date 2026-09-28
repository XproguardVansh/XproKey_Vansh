package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.CardApi
import com.xprokeey2.data.remote.dto.card.CardDto
import com.xprokeey2.data.remote.dto.card.CardRequestDto
import com.xprokeey2.data.remote.dto.card.CardUpdateRequestDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CardRemoteDataSourceImpl @Inject constructor(
    private val api: CardApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : CardRemoteDataSource {

    override suspend fun getCards(): Resource<List<CardDto>> =
        authorizedApiCall(tokenStore, json) { api.getCards(it).data.cards }

    override suspend fun getCard(id: Long): Resource<CardDto> =
        authorizedApiCall(tokenStore, json) { api.getCard(it, id).data }

    override suspend fun createCard(request: CardRequestDto): Resource<CardDto> =
        authorizedApiCall(tokenStore, json) { api.createCard(it, request).data }

    override suspend fun updateCard(id: Long, request: CardUpdateRequestDto): Resource<CardDto> =
        authorizedApiCall(tokenStore, json) { api.updateCard(it, id, request).data }

    override suspend fun deleteCard(id: Long): Resource<String> =
        authorizedApiCall(tokenStore, json) { api.deleteCard(it, id).message }
}
