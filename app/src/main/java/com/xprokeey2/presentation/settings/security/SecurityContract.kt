package com.xprokeey2.presentation.settings.security

import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Settings > Security: how long before the session times out, and what happens then. */
data class SecurityUiState(
    val user: UserBadge? = null,
    val isLoading: Boolean = true,
    val duration: TimeoutDuration = TimeoutDuration.NEVER,
    val action: TimeoutAction = TimeoutAction.LOGOUT,
    val isSaving: Boolean = false,
    /** "✓ Saved" on the button for a moment after saving, like the web. */
    val isSaved: Boolean = false,
)

sealed interface SecurityAction {
    data class DurationSelected(val duration: TimeoutDuration) : SecurityAction
    data class ActionSelected(val action: TimeoutAction) : SecurityAction
    data object Save : SecurityAction
}

sealed interface SecurityEvent {
    data class ShowMessage(val message: UiText) : SecurityEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : SecurityEvent
}
