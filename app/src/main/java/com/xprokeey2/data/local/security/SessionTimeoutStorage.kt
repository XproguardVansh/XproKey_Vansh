package com.xprokeey2.data.local.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xprokeey2.di.SessionTimeoutDataStore
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This device's copy of the session timeout and when the app was last used (the web keeps both in
 * localStorage under the same keys). Its own file, so signing out doesn't clear it; the web doesn't
 * clear those keys either.
 */
@Singleton
class SessionTimeoutStorage @Inject constructor(
    @param:SessionTimeoutDataStore private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<SessionTimeoutSettings?> = dataStore.data.map { prefs ->
        prefs[DURATION]?.let { duration ->
            SessionTimeoutSettings(
                duration = TimeoutDuration.of(duration) ?: TimeoutDuration.NEVER,
                action = TimeoutAction.of(prefs[ACTION]) ?: TimeoutAction.LOGOUT,
            )
        }
    }

    suspend fun get(): SessionTimeoutSettings? = settings.first()

    suspend fun save(settings: SessionTimeoutSettings) {
        dataStore.edit { prefs ->
            prefs[DURATION] = settings.duration.value
            prefs[ACTION] = settings.action.value
        }
    }

    /** Wall-clock time (ms) the signed-in app was last used; null if never recorded. */
    suspend fun getLastActivity(): Long? = dataStore.data.first()[LAST_ACTIVITY]

    suspend fun saveLastActivity(epochMillis: Long) {
        dataStore.edit { it[LAST_ACTIVITY] = epochMillis }
    }

    private companion object {
        val DURATION = stringPreferencesKey("xpk_session_timeout_duration")
        val ACTION = stringPreferencesKey("xpk_session_timeout_action")
        val LAST_ACTIVITY = longPreferencesKey("xpk_last_activity")
    }
}
