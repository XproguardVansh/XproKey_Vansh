package com.xprokeey2.presentation.dashboard

import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Only the Cards numbers are live for now; the other sections are placeholders. */
data class DashboardUiState(
    val user: UserBadge? = null,
    /** Null until loaded (or when loading failed): shown as "—". */
    val cardCount: Int? = null,
)

sealed interface DashboardAction {
    /** Screen became visible again, e.g. back from Cards. */
    data object Refresh : DashboardAction
}

sealed interface DashboardEvent {
    data class SessionExpired(val message: UiText) : DashboardEvent
}
