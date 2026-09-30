package com.xprokeey2.presentation.auth.lock

import com.xprokeey2.presentation.util.UiText

/** The Lock screen after a session timeout ("Lock"): the master password opens the vault again. */
data class LockUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val error: UiText? = null,
    val isUnlocking: Boolean = false,
)

sealed interface LockAction {
    data class PasswordChanged(val password: String) : LockAction
    data object TogglePasswordVisibility : LockAction
    data object Unlock : LockAction
    data object LogOut : LockAction
}

sealed interface LockEvent {
    /** Vault open again: on to the Dashboard, showing [message]. */
    data class Unlocked(val message: UiText) : LockEvent

    /** Session cleared: back to Login, showing [message] if there is one. */
    data class SignedOut(val message: UiText?) : LockEvent

    data class ShowMessage(val message: UiText) : LockEvent
}
