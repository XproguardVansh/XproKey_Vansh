package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.mapper.toStoredItem
import com.xprokeey2.data.remote.dto.vault.CreateCategoryResponseDto
import com.xprokeey2.data.remote.dto.vault.CustomCategoriesDto
import com.xprokeey2.data.remote.dto.vault.VaultCategoriesDto
import com.xprokeey2.data.remote.dto.vault.VaultItemListDto
import com.xprokeey2.data.remote.dto.vault.VaultItemResponseDto
import com.xprokeey2.data.remote.dto.vault.VaultItemUpdateRequestDto
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** The real responses of the vault endpoints (from Postman). */
class VaultDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val itemJson = """
        {"category": "Work", "created_at": "2026-09-28T10:51:07.080231Z", "id": 781, "is_favorite": false,
         "notes": "test account", "title": "gmail", "updated_at": "2026-09-28T10:51:07.080231Z",
         "url": "https://gmail.com", "username": "test@gmail.com"}
    """.trimIndent()

    @Test
    fun parsesTheList() {
        val item = json.decodeFromString<VaultItemListDto>("""{"items": [$itemJson]}""").items.single().toDomain()

        assertEquals(781L, item.id)
        assertEquals("gmail", item.title)
        assertEquals("test@gmail.com", item.username)
        assertEquals("https://gmail.com", item.url)
        assertEquals("test account", item.notes)
        assertEquals("Work", item.category)
        assertEquals(false, item.isFavorite)
        assertEquals(Instant.parse("2026-09-28T10:51:07.080231Z"), item.updatedAt)
    }

    @Test
    fun itemByIdCarriesThePassword() {
        val withPassword = itemJson.replace("\"notes\"", "\"password\": \"ENCRYPTED_TEST_STRING_123456\", \"notes\"")
        val stored = json.decodeFromString<VaultItemResponseDto>("""{"item": $withPassword}""").item.toStoredItem()
        assertEquals("ENCRYPTED_TEST_STRING_123456", stored.encryptedPassword)
    }

    @Test
    fun parsesCategories() {
        val builtIn = json.decodeFromString<VaultCategoriesDto>(
            """{"categories": ["Personal", "Work", "Finance", "Shopping", "Travel", "Social", "Others"]}"""
        )
        val custom = json.decodeFromString<CustomCategoriesDto>(
            """{"categories": [{"id": 47, "user_id": 532, "name": "DevOps", "created_at": "2026-09-28T12:41:46.008497Z"}]}"""
        )
        val created = json.decodeFromString<CreateCategoryResponseDto>(
            """{"category": {"created_at": "2026-09-28T12:41:46.008497Z", "id": 47, "name": "DevOps"}, "message": "Category created successfully"}"""
        )
        assertEquals(7, builtIn.categories.size)
        assertEquals("DevOps", custom.categories.single().name)
        assertEquals("DevOps", created.category.name)
    }

    @Test
    fun updateSendsOnlyChangedFields() {
        val body = json.encodeToJsonElement(
            VaultItemUpdateRequestDto.serializer(),
            VaultItemUpdatePayload(title = "jjsjdj", isFavorite = true).toRequestDto(),
        ).jsonObject

        assertEquals(setOf("title", "is_favorite"), body.keys)
    }
}
