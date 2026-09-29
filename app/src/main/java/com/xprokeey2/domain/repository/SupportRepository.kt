package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.CreatedTicket
import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketDraft
import com.xprokeey2.domain.util.Resource

interface SupportRepository {

    /** GET /support/my-tickets */
    suspend fun getMyTickets(): Resource<List<SupportTicket>>

    /** GET /support/tickets/{id} */
    suspend fun getTicket(id: String): Resource<SupportTicket>

    /** POST /support/tickets */
    suspend fun createTicket(draft: TicketDraft): Resource<CreatedTicket>
}
