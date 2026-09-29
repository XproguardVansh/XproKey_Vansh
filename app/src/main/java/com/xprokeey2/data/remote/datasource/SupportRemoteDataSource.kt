package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.support.CreateTicketRequestDto
import com.xprokeey2.data.remote.dto.support.CreateTicketResponseDto
import com.xprokeey2.data.remote.dto.support.SupportTicketDto
import com.xprokeey2.domain.util.Resource

interface SupportRemoteDataSource {
    suspend fun getMyTickets(): Resource<List<SupportTicketDto>>
    suspend fun getTicket(id: String): Resource<SupportTicketDto>
    suspend fun createTicket(request: CreateTicketRequestDto): Resource<CreateTicketResponseDto>
}
