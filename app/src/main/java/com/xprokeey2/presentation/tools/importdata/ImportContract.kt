package com.xprokeey2.presentation.tools.importdata

import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class ImportUiState(
    val user: UserBadge? = null,
    /** The chosen .csv, .json or .xpk; cleared after a successful import. */
    val file: PickedFile? = null,
    val isImporting: Boolean = false,
)

sealed interface ImportAction {
    /** [uri] comes from Android's "Open" screen. */
    data class FilePicked(val uri: String) : ImportAction

    data object Import : ImportAction

    /** "Open" couldn't be shown on this phone. */
    data object PickerUnavailable : ImportAction
}

sealed interface ImportEvent {
    data class ShowMessage(val message: UiText) : ImportEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : ImportEvent
}
