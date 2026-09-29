package com.xprokeey2.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The web support page's helpers (formatStatus/Category/Priority, search, stats). */
class SupportTicketTest {

    private fun ticket(id: String, status: String = "open", category: String = "general", number: String? = null) = SupportTicket(
        id = id, ticketNumber = number, name = "Vansh", email = "v@x.com", category = category,
        subject = "Vault sync issue", message = "Passwords missing", priority = "medium", status = status,
        createdAt = null, updatedAt = null,
    )

    @Test
    fun displayIdPrefersTheTicketNumber() {
        assertEquals("TICK-0014", ticket("14").displayId)
        assertEquals("TICK-12345", ticket("12345").displayId)
        assertEquals("SUP-7", ticket("7", number = "SUP-7").displayId)
        assertEquals("TICK-0007", ticket("7", number = "").displayId)
    }

    @Test
    fun searchLooksAtSubjectMessageAndNumber() {
        val plain = ticket("14")
        assertTrue(plain.matchesSearch("  VAULT "))
        assertTrue(plain.matchesSearch("missing"))
        assertTrue(plain.matchesSearch("14"))
        // Like the web, the id is searched, not the "TICK-0014" shown for it.
        assertFalse(plain.matchesSearch("TICK-0014"))
        assertTrue(ticket("7", number = "SUP-7").matchesSearch("sup-7"))
        assertTrue(plain.matchesSearch(""))
    }

    @Test
    fun unknownValuesShowLikeTheWeb() {
        assertEquals(TicketStatus.IN_PROGRESS, TicketStatus.of("IN_PROGRESS"))
        assertEquals(TicketStatus.OPEN, TicketStatus.of("pending"))
        assertEquals(TicketCategory.BUG_REPORT, TicketCategory.of("bug_report"))
        assertEquals(TicketCategory.GENERAL, TicketCategory.of(null))
        assertEquals(TicketPriority.URGENT, TicketPriority.of("Urgent"))
        assertEquals(TicketPriority.MEDIUM, TicketPriority.of(""))
    }

    @Test
    fun statsCountClosedAsResolved() {
        val stats = TicketStats.of(
            listOf(ticket("1"), ticket("2", "in_progress"), ticket("3", "resolved"), ticket("4", "closed"), ticket("5", "pending"))
        )
        assertEquals(TicketStats(total = 5, open = 1, inProgress = 1, resolved = 2), stats)
    }
}
