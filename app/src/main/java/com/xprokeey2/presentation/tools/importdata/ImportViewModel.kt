package com.xprokeey2.presentation.tools.importdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.transfer.GetPickedFileUseCase
import com.xprokeey2.domain.usecase.transfer.ImportVaultUseCase
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
class ImportViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getPickedFile: GetPickedFileUseCase,
    private val importVault: ImportVaultUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<ImportEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: ImportAction) {
        when (action) {
            is ImportAction.FilePicked -> viewModelScope.launch {
                when (val file = getPickedFile(action.uri)) {
                    null -> _events.send(ImportEvent.ShowMessage(UiText.Resource(R.string.import_failed)))
                    else -> _state.update { it.copy(file = file) }
                }
            }
            ImportAction.Import -> import()
            ImportAction.PickerUnavailable -> viewModelScope.launch {
                _events.send(ImportEvent.ShowMessage(UiText.Resource(R.string.import_failed)))
            }
        }
    }

    private fun import() {
        val current = _state.value
        if (current.isImporting) return
        val file = current.file
        if (file == null) {
            viewModelScope.launch { _events.send(ImportEvent.ShowMessage(UiText.Resource(R.string.import_select_file))) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            val result = importVault(file)
            _state.update { it.copy(isImporting = false) }
            val event = when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(file = null) }
                    ImportEvent.ShowMessage(UiText.Plural(R.plurals.import_done, result.data, result.data))
                }
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked -> ImportEvent.SignInRequired(error.asUiText())
                    is DataError.ImportFile, DataError.NoInternet, DataError.Timeout -> ImportEvent.ShowMessage(error.asUiText())
                    // The web's message for everything else.
                    else -> ImportEvent.ShowMessage(UiText.Resource(R.string.import_failed))
                }
            }
            _events.send(event)
        }
    }
}
