package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** "Save changes": the server first, then this device's copy, which restarts the timeout. */
class SaveSecuritySettingsUseCase @Inject constructor(
    private val repository: SecuritySettingsRepository,
) {
    suspend operator fun invoke(settings: SessionTimeoutSettings): Resource<Unit> {
        val result = repository.updateSettings(settings)
        if (result is Resource.Success) repository.saveSettings(settings)
        return result
    }
}
