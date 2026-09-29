package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.remote.dto.support.CreateTicketRequestDto
import com.xprokeey2.data.remote.dto.support.CreateTicketResponseDto
import com.xprokeey2.data.remote.dto.support.SupportTicketListDto
import com.xprokeey2.data.remote.dto.support.SupportTicketResponseDto
import com.xprokeey2.data.remote.util.safeApiCall
import com.xprokeey2.domain.model.TicketCategory
import com.xprokeey2.domain.model.TicketDraft
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant

/** lib/api/support.ts shapes. */
class SupportDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val ticketJson = """
        {"id": 14, "name": "Vansh", "email": "goelv2610@gmail.com", "category": "general",
         "subject": "Testing", "message": "Testing", "priority": "medium", "status": "open",
         "created_at": "2026-09-29T11:33:00Z", "updated_at": "2026-09-29T11:33:00Z"}
    """.trimIndent()

    @Test
    fun parsesATicket() {
        val ticket = json.decodeFromString<SupportTicketResponseDto>("""{"ticket": $ticketJson}""").ticket.toDomain()
        assertEquals("14", ticket.id)
        assertEquals("TICK-0014", ticket.displayId)
        assertEquals("Vansh", ticket.name)
        assertEquals("general", ticket.category)
        assertEquals("open", ticket.status)
        assertEquals(Instant.parse("2026-09-29T11:33:00Z"), ticket.createdAt)
    }

    @Test
    fun idMayBeAStringAndTheListMayBeEmpty() {
        val list = json.decodeFromString<SupportTicketListDto>(
            """{"tickets": [{"id": "abc-1", "ticket_number": "TICK-0099", "subject": "Hi"}], "total": 1}"""
        )
        val ticket = list.tickets!!.single().toDomain()
        assertEquals("abc-1", ticket.id)
        assertEquals("TICK-0099", ticket.displayId)
        assertNull(ticket.createdAt)
        assertNull(json.decodeFromString<SupportTicketListDto>("""{"tickets": null}""").tickets)
    }

    @Test
    fun createResponseWithAndWithoutTheTicket() {
        val withTicket = json.decodeFromString<CreateTicketResponseDto>("""{"message": "Ticket created", "ticket": $ticketJson}""")
        assertEquals("14", withTicket.ticket?.toDomain()?.id)
        assertNull(json.decodeFromString<CreateTicketResponseDto>("""{"message": "Ticket created"}""").ticket)
    }

    @Test
    fun requestBodyMatchesTheWebForm() {
        val body = json.encodeToJsonElement(
            CreateTicketRequestDto.serializer(),
            TicketDraft("Vansh", "goelv2610@gmail.com", TicketCategory.BUG_REPORT, "Crash", "Steps…").toRequestDto(),
        ).jsonObject
        assertEquals(setOf("name", "email", "category", "subject", "message", "priority"), body.keys)
        assertEquals("bug_report", body.getValue("category").jsonPrimitive.content)
        assertEquals("medium", body.getValue("priority").jsonPrimitive.content)
    }

    @Test
    fun errorTextComesFromErrorOrElseMessage() = runBlocking {
        fun failing(body: String) = HttpException(Response.error<Unit>(400, body.toResponseBody("application/json".toMediaType())))

        val fromMessage = safeApiCall(json) { throw failing("""{"message": "Subject is too long"}""") }
        assertEquals(Resource.Error(DataError.Server(400, "Subject is too long")), fromMessage)

        val fromError = safeApiCall(json) { throw failing("""{"error": "Invalid input", "message": "ignored"}""") }
        assertEquals(Resource.Error(DataError.Server(400, "Invalid input")), fromError)
    }
}
