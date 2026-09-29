package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toDto
import com.xprokeey2.data.remote.dto.transfer.ImportItemDto
import com.xprokeey2.data.remote.dto.transfer.ImportResponseDto
import com.xprokeey2.domain.model.ImportRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun importBodyIsAPlainArrayLikeTheWebSends() {
        val body = json.encodeToJsonElement(
            ListSerializer(ImportItemDto.serializer()),
            listOf(ImportRecord("Github", "Vanshgoel2610", "CT", "https://github.com", "", "Personal").toDto()),
        )
        val item = body.jsonArray.single().jsonObject
        assertEquals(setOf("title", "username", "password", "url", "notes", "category"), item.keys)
    }

    @Test
    fun parsesTheImportResponse() {
        val response = json.decodeFromString<ImportResponseDto>("""{"message": "Imported 8 items successfully", "imported": 8}""")
        assertEquals(8, response.imported)
    }
}
