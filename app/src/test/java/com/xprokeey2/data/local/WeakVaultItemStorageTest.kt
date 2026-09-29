package com.xprokeey2.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.xprokeey2.data.local.vault.WeakVaultItemStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** Same behaviour as the web's `markVaultItemWeakness` / `removeVaultItemWeakness`. */
class WeakVaultItemStorageTest {

    /** DataStore's file storage can't replace files on Windows JVMs, so the test keeps it in memory. */
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    @Test
    fun marksAndRemovesItems() = runBlocking {
        val storage = WeakVaultItemStorage(InMemoryDataStore())
        assertEquals(emptySet<Long>(), storage.getWeakItemIds())

        storage.markWeakness(781, isWeak = true)
        storage.markWeakness(782, isWeak = true)
        storage.markWeakness(782, isWeak = false) // its password was changed to a strong one
        storage.remove(900) // not in the list: nothing happens
        assertEquals(setOf(781L), storage.getWeakItemIds())

        storage.remove(781)
        assertEquals(emptySet<Long>(), storage.getWeakItemIds())
    }
}
