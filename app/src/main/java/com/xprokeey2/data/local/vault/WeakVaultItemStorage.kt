package com.xprokeey2.data.local.vault

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.xprokeey2.di.VaultSecurityDataStore
import com.xprokeey2.domain.repository.WeakVaultItemRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Only item ids are stored, never passwords. Its own file, so signing out doesn't clear it (the
 * web's localStorage key isn't cleared either).
 */
@Singleton
class WeakVaultItemStorage @Inject constructor(
    @param:VaultSecurityDataStore private val dataStore: DataStore<Preferences>,
) : WeakVaultItemRepository {

    override suspend fun getWeakItemIds(): Set<Long> =
        dataStore.data.first()[WEAK_ITEM_IDS].orEmpty().mapNotNullTo(mutableSetOf()) { it.toLongOrNull() }

    override suspend fun markWeakness(itemId: Long, isWeak: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[WEAK_ITEM_IDS].orEmpty()
            prefs[WEAK_ITEM_IDS] = if (isWeak) current + itemId.toString() else current - itemId.toString()
        }
    }

    override suspend fun remove(itemId: Long) = markWeakness(itemId, isWeak = false)

    private companion object {
        val WEAK_ITEM_IDS = stringSetPreferencesKey("xprokey_weak_vault_ids")
    }
}
