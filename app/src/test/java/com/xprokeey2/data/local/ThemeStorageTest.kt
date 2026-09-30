package com.xprokeey2.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xprokeey2.data.local.theme.ThemeStorage
import com.xprokeey2.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ThemeStorageTest {

    /** DataStore's file storage can't replace files on Windows JVMs, so the test keeps it in memory. */
    private class InMemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    private class UnreadableDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("corrupt file") }
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("corrupt file")
    }

    @Test
    fun followsThePhoneUntilAChoiceIsSaved() = runBlocking {
        val storage = ThemeStorage(InMemoryDataStore())
        assertEquals(ThemeMode.SYSTEM, storage.observeThemeMode().first())

        storage.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, storage.observeThemeMode().first())

        storage.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, storage.observeThemeMode().first())
    }

    @Test
    fun unknownOrUnreadableValuesFollowThePhone() = runBlocking {
        val unknown = InMemoryDataStore(mutablePreferencesOf(stringPreferencesKey("theme") to "sepia"))
        assertEquals(ThemeMode.SYSTEM, ThemeStorage(unknown).observeThemeMode().first())

        // The first screen waits for this value, so a broken file must not stop the app from opening.
        assertEquals(ThemeMode.SYSTEM, ThemeStorage(UnreadableDataStore()).observeThemeMode().first())
    }
}
