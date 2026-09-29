package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.remote.dto.vault.CustomCategoryDto
import com.xprokeey2.data.remote.dto.vault.VaultItemDto
import com.xprokeey2.data.remote.dto.vault.VaultItemRequestDto
import com.xprokeey2.data.remote.dto.vault.VaultItemUpdateRequestDto
import com.xprokeey2.domain.util.Resource

interface VaultRemoteDataSource {
    suspend fun getItems(): Resource<List<VaultItemDto>>
    suspend fun getItem(id: Long): Resource<VaultItemDto>
    suspend fun createItem(request: VaultItemRequestDto): Resource<VaultItemDto>

    /** Returns the server message. */
    suspend fun updateItem(id: Long, request: VaultItemUpdateRequestDto): Resource<String>

    /** Returns the server message. */
    suspend fun deleteItem(id: Long): Resource<String>

    suspend fun getCategories(): Resource<List<String>>
    suspend fun getCustomCategories(): Resource<List<CustomCategoryDto>>
    suspend fun createCategory(name: String): Resource<CustomCategoryDto>
}
