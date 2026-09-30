package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Opening Settings > Security, like the web page: the server's settings (missing values become "never"
 * and "logout"), also saved on this device. If the server can't be reached, this device's copy is shown;
 * only a finished session is an error.
 */
class GetSecuritySettingsUseCase @Inject constructor(
    private val repository: SecuritySettingsRepository,
) {
    suspend operator fun invoke(): Resource<SessionTimeoutSettings> =
        when (val result = repository.fetchSettings()) {
            is Resource.Success -> {
                val settings = SessionTimeoutSettings(
                    duration = result.data.duration ?: TimeoutDuration.NEVER,
                    action = result.data.action ?: TimeoutAction.LOGOUT,
                )
                repository.saveSettings(settings)
                Resource.Success(settings)
            }
            is Resource.Error -> when (result.error) {
                DataError.SessionExpired -> result
                else -> Resource.Success(repository.getSavedSettings() ?: SessionTimeoutSettings())
            }
        }
}
