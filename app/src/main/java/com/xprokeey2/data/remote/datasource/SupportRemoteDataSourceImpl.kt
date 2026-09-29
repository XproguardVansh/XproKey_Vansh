package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.SupportApi
import com.xprokeey2.data.remote.dto.support.CreateTicketRequestDto
import com.xprokeey2.data.remote.dto.support.CreateTicketResponseDto
import com.xprokeey2.data.remote.dto.support.SupportTicketDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SupportRemoteDataSourceImpl @Inject constructor(
    private val api: SupportApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : SupportRemoteDataSource {

    override suspend fun getMyTickets(): Resource<List<SupportTicketDto>> =
        authorizedApiCall(tokenStore, json) { api.getMyTickets(it).tickets.orEmpty() }

    override suspend fun getTicket(id: String): Resource<SupportTicketDto> =
        authorizedApiCall(tokenStore, json) { api.getTicket(it, id).ticket }

    override suspend fun createTicket(request: CreateTicketRequestDto): Resource<CreateTicketResponseDto> =
        authorizedApiCall(tokenStore, json) { api.createTicket(it, request) }
}
