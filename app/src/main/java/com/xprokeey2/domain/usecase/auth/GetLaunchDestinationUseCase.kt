package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.LaunchDestination
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Opening the app: someone signed in goes straight in, like the web, unless the Settings > Security
 * timeout passed since the app was last used (time with the app closed counts). Then "Log out" signs
 * out and shows Login, and "Lock" shows the Lock screen. With "Never" the user stays in until they log out.
 */
class GetLaunchDestinationUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val securitySettings: SecuritySettingsRepository,
    private val signOut: SignOutUseCase,
) {
    suspend operator fun invoke(nowMillis: Long): LaunchDestination {
        if (!sessionRepository.hasSession()) return LaunchDestination.LOGIN

        val settings = securitySettings.getSavedSettings() ?: SessionTimeoutSettings()
        val minutes = settings.duration.minutes ?: return LaunchDestination.APP
        val idleMillis = securitySettings.getLastActivity()?.let { nowMillis - it }
        // No recorded use, or a clock turned back, counts as timed out.
        val timedOut = idleMillis == null || idleMillis < 0 || idleMillis >= minutes * 60_000L
        if (!timedOut) return LaunchDestination.APP

        return when (settings.action) {
            TimeoutAction.LOCK -> LaunchDestination.LOCK
            TimeoutAction.LOGOUT -> {
                signOut()
                LaunchDestination.LOGIN
            }
        }
    }
}
