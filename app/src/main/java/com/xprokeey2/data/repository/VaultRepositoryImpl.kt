package com.xprokeey2.data.repository

import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.mapper.toRequestDto
import com.xprokeey2.data.mapper.toStoredItem
import com.xprokeey2.data.remote.datasource.VaultRemoteDataSource
import com.xprokeey2.domain.model.StoredVaultItem
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemPayload
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class VaultRepositoryImpl @Inject constructor(
    private val remote: VaultRemoteDataSource,
) : VaultRepository {

    override suspend fun getItems(): Resource<List<VaultItem>> =
        remote.getItems().map { items -> items.map { it.toDomain() } }

    override suspend fun getItem(id: Long): Resource<StoredVaultItem> =
        remote.getItem(id).map { it.toStoredItem() }

    override suspend fun createItem(payload: VaultItemPayload): Resource<VaultItem> =
        remote.createItem(payload.toRequestDto()).map { it.toDomain() }

    override suspend fun updateItem(id: Long, payload: VaultItemUpdatePayload): Resource<String> =
        remote.updateItem(id, payload.toRequestDto())

    override suspend fun deleteItem(id: Long): Resource<String> = remote.deleteItem(id)

    override suspend fun getCategories(): Resource<List<String>> {
        val builtIn = when (val result = remote.getCategories()) {
            is Resource.Success -> result.data
            is Resource.Error -> return result
        }
        val custom = when (val result = remote.getCustomCategories()) {
            is Resource.Success -> result.data.map { it.name }
            is Resource.Error -> return result
        }
        return Resource.Success((builtIn + custom).filter { it.isNotBlank() }.distinct())
    }

    override suspend fun createCategory(name: String): Resource<String> =
        remote.createCategory(name).map { it.name }
}
