package com.xprokeey2.data.local.theme

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xprokeey2.di.ThemeDataStore
import com.xprokeey2.domain.model.ThemeMode
import com.xprokeey2.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Its own file, so signing out doesn't clear it; the web doesn't clear its "theme" key either. */
@Singleton
class ThemeStorage @Inject constructor(
    @param:ThemeDataStore private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data
        // The first screen waits for this value, so an unreadable file means the default, not a hang.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> ThemeMode.of(prefs[THEME]) ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[THEME] = mode.value }
    }

    private companion object {
        /** next-themes' localStorage key. */
        val THEME = stringPreferencesKey("theme")
    }
}
