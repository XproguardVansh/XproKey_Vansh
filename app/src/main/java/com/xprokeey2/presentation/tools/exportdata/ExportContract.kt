package com.xprokeey2.presentation.tools.exportdata

import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class ExportUiState(
    val user: UserBadge? = null,
    /** Formats being downloaded and saved; their button shows "Preparing…". */
    val preparing: Set<ExportFormat> = emptySet(),
)

sealed interface ExportAction {
    /** The user chose where to save [format]: [uri] comes from Android's "Save as" screen. */
    data class SaveTo(val format: ExportFormat, val uri: String) : ExportAction

    /** "Save as" couldn't be opened on this phone. */
    data object SaveScreenUnavailable : ExportAction
}

sealed interface ExportEvent {
    data class ShowMessage(val message: UiText) : ExportEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : ExportEvent
}
