package com.xprokeey2.presentation.passwords.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.vault.DeleteVaultItemsUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultItemDetailsUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.PasswordDetailsRoute
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasswordDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItemDetails: GetVaultItemDetailsUseCase,
    private val deleteItems: DeleteVaultItemsUseCase,
) : ViewModel() {

    private val itemId = savedStateHandle.toRoute<PasswordDetailsRoute>().itemId

    private val _state = MutableStateFlow(PasswordDetailsUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<PasswordDetailsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: PasswordDetailsAction) {
        when (action) {
            PasswordDetailsAction.Refresh -> load()
            PasswordDetailsAction.TogglePassword -> togglePassword()
            is PasswordDetailsAction.Copy -> copy(action.field)
            PasswordDetailsAction.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            PasswordDetailsAction.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            PasswordDetailsAction.DeleteConfirmed -> confirmDelete()
        }
    }

    private fun load() {
        viewModelScope.launch {
            // The password is hidden again whenever the screen comes back.
            _state.update { it.copy(isLoading = it.details == null, loadError = null, isPasswordVisible = false) }
            val result = getItemDetails(itemId)
            _state.update { it.copy(isLoading = false) }
            when (result) {
                is Resource.Success -> _state.update { it.copy(details = result.data) }
                is Resource.Error -> handleError(result.error) { message ->
                    if (_state.value.details == null) {
                        _state.update { it.copy(loadError = message) }
                    } else {
                        _events.send(PasswordDetailsEvent.ShowMessage(message))
                    }
                }
            }
        }
    }

    private fun togglePassword() {
        val details = _state.value.details ?: return
        if (details.password == null) {
            viewModelScope.launch { _events.send(PasswordDetailsEvent.ShowMessage(DecryptFailed)) }
            return
        }
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private fun copy(field: VaultField) {
        val details = _state.value.details ?: return
        val item = details.item
        val (label, value) = when (field) {
            VaultField.USERNAME -> R.string.label_username_email to item.username
            VaultField.WEBSITE -> R.string.label_website to item.url
            VaultField.PASSWORD -> R.string.label_password to details.password
            VaultField.NOTES -> R.string.label_notes to item.notes
        }
        viewModelScope.launch {
            if (value == null) {
                _events.send(PasswordDetailsEvent.ShowMessage(DecryptFailed))
            } else {
                _events.send(
                    PasswordDetailsEvent.CopyToClipboard(
                        label = UiText.Resource(label),
                        value = value,
                        sensitive = field == VaultField.PASSWORD,
                    )
                )
            }
        }
    }

    private fun confirmDelete() {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            val result = deleteItems(listOf(itemId))
            _state.update { it.copy(isDeleting = false, isDeleteDialogVisible = false) }
            when (result) {
                is Resource.Success -> _events.send(
                    PasswordDetailsEvent.Deleted(UiText.Plural(R.plurals.passwords_deleted, 1, 1))
                )
                is Resource.Error -> handleError(result.error) { _events.send(PasswordDetailsEvent.ShowMessage(it)) }
            }
        }
    }

    private suspend fun handleError(error: DataError, show: suspend (UiText) -> Unit) {
        when (error) {
            DataError.SessionExpired, DataError.VaultLocked ->
                _events.send(PasswordDetailsEvent.SignInRequired(error.asUiText()))
            else -> show(error.asUiText())
        }
    }

    private companion object {
        val DecryptFailed = UiText.Resource(R.string.password_decrypt_failed)
    }
}
