package com.xprokeey2.presentation.support

import com.xprokeey2.domain.model.SupportTicket
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.domain.model.TicketStatus
import com.xprokeey2.presentation.support.list.SupportEmptyState
import com.xprokeey2.presentation.support.list.SupportUiState
import org.junit.Assert.assertEquals
import org.junit.Test

/** The web list's tabs, category menu, search and empty messages. */
class SupportUiStateTest {

    private fun ticket(id: String, status: String, category: String, subject: String = "Ticket $id") = SupportTicket(
        id = id, ticketNumber = null, name = "", email = "", category = category, subject = subject,
        message = "", priority = "medium", status = status, createdAt = null, updatedAt = null,
    )

    private val tickets = listOf(
        ticket("1", "open", "technical", "Vault sync issue"),
        ticket("2", "in_progress", "billing"),
        ticket("3", "resolved", "general"),
    )

    @Test
    fun filtersByTabCategoryAndSearch() {
        val state = SupportUiState(tickets = tickets)
        assertEquals(listOf("1", "2", "3"), state.visibleTickets.map { it.id })
        assertEquals(listOf("2"), state.copy(statusFilter = TicketStatus.IN_PROGRESS).visibleTickets.map { it.id })
        assertEquals(listOf("3"), state.copy(categoryFilter = TicketCategory.GENERAL).visibleTickets.map { it.id })
        assertEquals(listOf("1"), state.copy(query = "sync").visibleTickets.map { it.id })
        assertEquals(emptyList<String>(), state.copy(statusFilter = TicketStatus.OPEN, categoryFilter = TicketCategory.BILLING).visibleTickets.map { it.id })
    }

    @Test
    fun emptyMessages() {
        assertEquals(SupportEmptyState.NO_TICKETS, SupportUiState().emptyState)
        assertEquals(SupportEmptyState.NONE_IN_PROGRESS, SupportUiState(tickets = tickets, statusFilter = TicketStatus.IN_PROGRESS).emptyState)
        assertEquals(SupportEmptyState.NONE_CLOSED, SupportUiState(tickets = tickets, statusFilter = TicketStatus.CLOSED).emptyState)
        assertEquals(SupportEmptyState.NO_MATCH, SupportUiState(tickets = tickets, statusFilter = TicketStatus.RESOLVED).emptyState)
    }
}
