package com.xprokeey2.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.card.GetCardsUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getCards: GetCardsUseCase,
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
            DashboardAction.Refresh -> loadCardCount()
        }
    }

    private fun loadCardCount() {
        viewModelScope.launch {
            when (val result = getCards()) {
                is Resource.Success -> _state.update { it.copy(cardCount = result.data.size) }
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired -> _events.send(DashboardEvent.SessionExpired(error.asUiText()))
                    // Keep the last count (or "—"); the Cards screen shows the actual error.
                    else -> Unit
                }
            }
        }
    }
}
