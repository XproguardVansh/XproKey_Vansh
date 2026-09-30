package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * The web app layout's check when the signed-in app opens: copy the server's session timeout to this
 * device. On a device that has never saved one, a missing or "15" minute timeout (the server's default)
 * becomes "never" and is sent back to the server. If the server can't be reached, a device without a
 * copy gets "never" and "logout".
 */
class SyncSessionTimeoutUseCase @Inject constructor(
    private val repository: SecuritySettingsRepository,
) {
    suspend operator fun invoke() {
        when (val result = repository.fetchSettings()) {
            is Resource.Success -> {
                var duration = result.data.duration
                var action = result.data.action
                val hasSaved = repository.getSavedSettings() != null
                if (!hasSaved && (duration == null || duration == TimeoutDuration.FIFTEEN_MINUTES)) {
                    duration = TimeoutDuration.NEVER
                    action = action ?: TimeoutAction.LOGOUT
                    repository.updateSettings(SessionTimeoutSettings(duration, action)) // Errors ignored, like the web.
                }
                repository.saveSettings(
                    SessionTimeoutSettings(
                        duration = duration ?: TimeoutDuration.NEVER,
                        action = action ?: TimeoutAction.LOGOUT,
                    )
                )
            }
            is Resource.Error -> if (repository.getSavedSettings() == null) {
                repository.saveSettings(SessionTimeoutSettings())
            }
        }
    }
}
