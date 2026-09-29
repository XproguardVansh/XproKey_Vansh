package com.xprokeey2.presentation.tools.exportdata

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.domain.usecase.transfer.ExportVaultUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val exportVault: ExportVaultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<ExportEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: ExportAction) {
        when (action) {
            is ExportAction.SaveTo -> export(action.format, action.uri)
            ExportAction.SaveScreenUnavailable -> viewModelScope.launch {
                _events.send(ExportEvent.ShowMessage(UiText.Resource(R.string.export_failed)))
            }
        }
    }

    private fun export(format: ExportFormat, uri: String) {
        if (format in _state.value.preparing) return
        viewModelScope.launch {
            _state.update { it.copy(preparing = it.preparing + format) }
            val result = exportVault(format, uri)
            _state.update { it.copy(preparing = it.preparing - format) }
            val event = when (result) {
                is Resource.Success -> ExportEvent.ShowMessage(UiText.Resource(format.doneMessage))
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked -> ExportEvent.SignInRequired(error.asUiText())
                    DataError.NoInternet, DataError.Timeout -> ExportEvent.ShowMessage(error.asUiText())
                    // The web's message for everything else.
                    else -> ExportEvent.ShowMessage(UiText.Resource(R.string.export_failed))
                }
            }
            _events.send(event)
        }
    }

    @get:StringRes
    private val ExportFormat.doneMessage: Int
        get() = when (this) {
            ExportFormat.CSV -> R.string.export_csv_done
            ExportFormat.JSON -> R.string.export_json_done
            ExportFormat.ENCRYPTED -> R.string.export_xpk_done
        }
}
