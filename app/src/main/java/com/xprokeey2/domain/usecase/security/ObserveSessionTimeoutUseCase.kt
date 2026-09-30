package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** The session timeout in force on this device: its saved copy, or the web's defaults. */
class ObserveSessionTimeoutUseCase @Inject constructor(
    private val repository: SecuritySettingsRepository,
) {
    operator fun invoke(): Flow<SessionTimeoutSettings> =
        repository.observeSavedSettings().map { it ?: SessionTimeoutSettings() }
}
