package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.StoredVaultItem
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemPayload
import com.xprokeey2.domain.model.VaultItemUpdatePayload
import com.xprokeey2.domain.util.Resource

interface VaultRepository {

    suspend fun getItems(): Resource<List<VaultItem>>

    /** One item including its encrypted password. */
    suspend fun getItem(id: Long): Resource<StoredVaultItem>

    suspend fun createItem(payload: VaultItemPayload): Resource<VaultItem>

    /** Sends only the fields set in [payload]. Returns the server message. */
    suspend fun updateItem(id: Long, payload: VaultItemUpdatePayload): Resource<String>

    /** Returns the server message. */
    suspend fun deleteItem(id: Long): Resource<String>

    /** The built-in categories followed by the user's own ones. */
    suspend fun getCategories(): Resource<List<String>>

    /** Returns the name of the new category. */
    suspend fun createCategory(name: String): Resource<String>
}
