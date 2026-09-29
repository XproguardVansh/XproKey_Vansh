package com.xprokeey2.presentation.dashboard

import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultSecurity
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Numbers stay null (shown as "—") until loaded, or when loading failed. */
data class DashboardUiState(
    val user: UserBadge? = null,
    val cardCount: Int? = null,
    val passwordCount: Int? = null,
    /** Null until the passwords are loaded. */
    val security: VaultSecurity? = null,
    /** Most recently updated passwords; null until loaded. */
    val recentItems: List<VaultItem>? = null,
)

sealed interface DashboardAction {
    /** Screen became visible again, e.g. back from Cards or Passwords. */
    data object Refresh : DashboardAction
}

sealed interface DashboardEvent {
    data class SessionExpired(val message: UiText) : DashboardEvent
}
