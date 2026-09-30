package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.repository.SecuritySettingsRepository
import javax.inject.Inject

/** Remembers when the signed-in app was last used, so the session timeout also counts time it was closed. */
class RecordLastActivityUseCase @Inject constructor(
    private val repository: SecuritySettingsRepository,
) {
    suspend operator fun invoke(epochMillis: Long) = repository.saveLastActivity(epochMillis)
}
