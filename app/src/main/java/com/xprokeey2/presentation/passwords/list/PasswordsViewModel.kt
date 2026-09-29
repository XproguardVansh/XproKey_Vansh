package com.xprokeey2.presentation.passwords.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemChanges
import com.xprokeey2.domain.model.VaultSecurity
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.vault.DeleteVaultItemsUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultCategoriesUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultItemsUseCase
import com.xprokeey2.domain.usecase.vault.GetWeakVaultItemIdsUseCase
import com.xprokeey2.domain.usecase.vault.UpdateVaultItemUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.PasswordsRoute
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
class PasswordsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getItems: GetVaultItemsUseCase,
    private val getCategories: GetVaultCategoriesUseCase,
    private val getWeakItemIds: GetWeakVaultItemIdsUseCase,
    private val updateItem: UpdateVaultItemUseCase,
    private val deleteItems: DeleteVaultItemsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PasswordsUiState(
            filter = if (savedStateHandle.toRoute<PasswordsRoute>().showWeakItems) {
                PasswordFilter.Weak
            } else {
                PasswordFilter.All
            },
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PasswordsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: PasswordsAction) {
        when (action) {
            PasswordsAction.Refresh -> refresh()
            is PasswordsAction.QueryChanged -> _state.update { it.copy(query = action.query) }
            is PasswordsAction.FilterSelected -> _state.update { it.copy(filter = action.filter, selectedIds = emptySet()) }
            is PasswordsAction.ToggleSelected -> _state.update {
                val selected = if (action.itemId in it.selectedIds) it.selectedIds - action.itemId else it.selectedIds + action.itemId
                it.copy(selectedIds = selected)
            }
            PasswordsAction.ToggleSelectAll -> _state.update {
                val visible = it.visibleItems.map(VaultItem::id).toSet()
                it.copy(selectedIds = if (visible.isNotEmpty() && it.selectedIds.containsAll(visible)) emptySet() else visible)
            }
            PasswordsAction.ClearSelection -> _state.update { it.copy(selectedIds = emptySet()) }
            is PasswordsAction.ToggleFavorite -> toggleFavorite(action.itemId)
            is PasswordsAction.CopyUsername -> viewModelScope.launch {
                _events.send(PasswordsEvent.CopyToClipboard(UiText.Resource(R.string.label_username), action.item.username))
            }
            PasswordsAction.DeleteClicked -> if (_state.value.selectedIds.isNotEmpty()) {
                _state.update { it.copy(isDeleteDialogVisible = true) }
            }
            PasswordsAction.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            PasswordsAction.DeleteConfirmed -> deleteSelected()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.items.isEmpty()) }
            when (val result = getItems()) {
                is Resource.Success -> {
                    val items = result.data
                    val weakIds = VaultSecurity.weakItemIds(items, getWeakItemIds())
                    val ids = items.map(VaultItem::id).toSet()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            weakItemIds = weakIds,
                            loadError = null,
                            selectedIds = it.selectedIds intersect ids,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    handleError(result.error) { message ->
                        if (_state.value.items.isEmpty()) {
                            _state.update { it.copy(loadError = message) }
                        } else {
                            _events.send(PasswordsEvent.ShowMessage(message))
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            // Only for the filter menu; the list works without it.
            val categories = getCategories()
            if (categories is Resource.Success) _state.update { it.copy(categories = categories.data) }
        }
    }

    /** The row's star, like the web list: it flips at once and flips back if saving fails. */
    private fun toggleFavorite(itemId: Long) {
        val current = _state.value
        if (itemId in current.favoriteUpdatingIds) return
        val item = current.items.firstOrNull { it.id == itemId } ?: return
        val favorite = !item.isFavorite
        _state.update { it.withFavorite(itemId, favorite).copy(favoriteUpdatingIds = it.favoriteUpdatingIds + itemId) }
        viewModelScope.launch {
            val result = updateItem(itemId, VaultItemChanges(isFavorite = favorite))
            val saved = if (result is Resource.Success) favorite else !favorite
            _state.update { it.withFavorite(itemId, saved).copy(favoriteUpdatingIds = it.favoriteUpdatingIds - itemId) }
            if (result is Resource.Error) handleError(result.error) { _events.send(PasswordsEvent.ShowMessage(it)) }
        }
    }

    private fun PasswordsUiState.withFavorite(itemId: Long, favorite: Boolean) =
        copy(items = items.map { if (it.id == itemId) it.copy(isFavorite = favorite) else it })

    private fun deleteSelected() {
        val ids = _state.value.selectedIds
        if (ids.isEmpty() || _state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            val result = deleteItems(ids)
            _state.update { it.copy(isDeleting = false, isDeleteDialogVisible = false, selectedIds = emptySet()) }
            when (result) {
                is Resource.Success -> _events.send(
                    PasswordsEvent.ShowMessage(UiText.Plural(R.plurals.passwords_deleted, result.data, result.data))
                )
                is Resource.Error -> handleError(result.error) { _events.send(PasswordsEvent.ShowMessage(it)) }
            }
            refresh()
        }
    }

    private suspend fun handleError(error: DataError, show: suspend (UiText) -> Unit) {
        when (error) {
            DataError.SessionExpired, DataError.VaultLocked -> _events.send(PasswordsEvent.SignInRequired(error.asUiText()))
            else -> show(error.asUiText())
        }
    }
}
