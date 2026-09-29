package com.xprokeey2.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.model.VaultSecurity
import com.xprokeey2.domain.usecase.card.GetCardsUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultItemsUseCase
import com.xprokeey2.domain.usecase.vault.GetWeakVaultItemIdsUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
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

/** How many passwords "Recently updated" lists (web: `slice(0, 4)`). */
private const val RECENT_ITEMS = 4

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getCards: GetCardsUseCase,
    private val getVaultItems: GetVaultItemsUseCase,
    private val getWeakItemIds: GetWeakVaultItemIdsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<DashboardEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> {
                loadCardCount()
                loadPasswords()
            }
        }
    }

    private fun loadCardCount() {
        viewModelScope.launch {
            when (val result = getCards()) {
                is Resource.Success -> _state.update { it.copy(cardCount = result.data.size) }
                is Resource.Error -> handleError(result.error)
            }
        }
    }

    private fun loadPasswords() {
        viewModelScope.launch {
            when (val result = getVaultItems()) {
                is Resource.Success -> {
                    val items = result.data
                    // Same inputs as the web dashboard's calcSecurityScore.
                    val security = VaultSecurity.calculate(
                        items = items,
                        missingFieldsCount = VaultSecurity.missingFieldsCount(items),
                        markedWeakIds = getWeakItemIds(),
                    )
                    _state.update {
                        it.copy(
                            passwordCount = items.size,
                            recentItems = items.sortedByDescending { item -> item.updatedAt }.take(RECENT_ITEMS),
                            security = security,
                        )
                    }
                }
                is Resource.Error -> handleError(result.error)
            }
        }
    }

    /** Keep the last numbers (or "—"); the sections themselves show the actual error. */
    private suspend fun handleError(error: DataError) {
        if (error == DataError.SessionExpired) _events.send(DashboardEvent.SessionExpired(error.asUiText()))
    }
}
