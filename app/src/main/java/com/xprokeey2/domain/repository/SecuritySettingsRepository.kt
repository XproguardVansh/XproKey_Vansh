package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.ServerSessionTimeout
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.flow.Flow

/** Settings > Security: the session timeout, kept on the server and copied to this device like the web's localStorage. */
interface SecuritySettingsRepository {

    /** GET /me/security */
    suspend fun fetchSettings(): Resource<ServerSessionTimeout>

    /** PUT /me/security */
    suspend fun updateSettings(settings: SessionTimeoutSettings): Resource<Unit>

    /** This device's copy; null until one was saved here. */
    suspend fun getSavedSettings(): SessionTimeoutSettings?

    suspend fun saveSettings(settings: SessionTimeoutSettings)

    /** This device's copy as it changes; null until one was saved here. */
    fun observeSavedSettings(): Flow<SessionTimeoutSettings?>
}
