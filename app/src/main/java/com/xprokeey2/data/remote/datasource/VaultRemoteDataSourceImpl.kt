package com.xprokeey2.data.remote.datasource

import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.remote.api.VaultApi
import com.xprokeey2.data.remote.dto.vault.CreateCategoryRequestDto
import com.xprokeey2.data.remote.dto.vault.CustomCategoryDto
import com.xprokeey2.data.remote.dto.vault.VaultItemDto
import com.xprokeey2.data.remote.dto.vault.VaultItemRequestDto
import com.xprokeey2.data.remote.dto.vault.VaultItemUpdateRequestDto
import com.xprokeey2.data.remote.util.authorizedApiCall
import com.xprokeey2.domain.util.Resource
import kotlinx.serialization.json.Json
import javax.inject.Inject

class VaultRemoteDataSourceImpl @Inject constructor(
    private val api: VaultApi,
    private val tokenStore: AccessTokenStore,
    private val json: Json,
) : VaultRemoteDataSource {

    override suspend fun getItems(): Resource<List<VaultItemDto>> =
        authorizedApiCall(tokenStore, json) { api.getItems(it).items }

    override suspend fun getItem(id: Long): Resource<VaultItemDto> =
        authorizedApiCall(tokenStore, json) { api.getItem(it, id).item }

    override suspend fun createItem(request: VaultItemRequestDto): Resource<VaultItemDto> =
        authorizedApiCall(tokenStore, json) { api.createItem(it, request).item }

    override suspend fun updateItem(id: Long, request: VaultItemUpdateRequestDto): Resource<String> =
        authorizedApiCall(tokenStore, json) { api.updateItem(it, id, request).message }

    override suspend fun deleteItem(id: Long): Resource<String> =
        authorizedApiCall(tokenStore, json) { api.deleteItem(it, id).message }

    override suspend fun getCategories(): Resource<List<String>> =
        authorizedApiCall(tokenStore, json) { api.getCategories(it).categories }

    override suspend fun getCustomCategories(): Resource<List<CustomCategoryDto>> =
        authorizedApiCall(tokenStore, json) { api.getCustomCategories(it).categories }

    override suspend fun createCategory(name: String): Resource<CustomCategoryDto> =
        authorizedApiCall(tokenStore, json) { api.createCategory(it, CreateCategoryRequestDto(name)).category }
}
