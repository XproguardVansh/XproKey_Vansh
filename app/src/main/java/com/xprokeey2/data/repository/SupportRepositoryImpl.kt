package com.xprokeey2.data.repository

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.remote.datasource.SupportRemoteDataSource
import com.xprokeey2.domain.model.CreatedTicket
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketDraft
import com.xprokeey2.domain.repository.SupportRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class SupportRepositoryImpl @Inject constructor(
    private val remote: SupportRemoteDataSource,
) : SupportRepository {

    override suspend fun getMyTickets(): Resource<List<SupportTicket>> =
        remote.getMyTickets().map { tickets -> tickets.map { it.toDomain() } }

    override suspend fun getTicket(id: String): Resource<SupportTicket> = remote.getTicket(id).map { it.toDomain() }

    override suspend fun createTicket(draft: TicketDraft): Resource<CreatedTicket> =
        remote.createTicket(draft.toRequestDto()).map { CreatedTicket(message = it.message, ticket = it.ticket?.toDomain()) }
}
