package com.xprokeey2.presentation.support.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.support.GetSupportTicketsUseCase
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
class SupportViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getTickets: GetSupportTicketsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SupportUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<SupportEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: SupportAction) {
        when (action) {
            SupportAction.Refresh -> refresh()
            is SupportAction.QueryChanged -> _state.update { it.copy(query = action.query) }
            is SupportAction.StatusSelected -> _state.update { it.copy(statusFilter = action.status) }
            is SupportAction.CategorySelected -> _state.update { it.copy(categoryFilter = action.category) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.tickets.isEmpty(), loadError = null) }
            when (val result = getTickets()) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, tickets = result.data, loadError = null) }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    val error = result.error
                    when {
                        error == DataError.SessionExpired -> _events.send(SupportEvent.SignInRequired(error.asUiText()))
                        _state.value.tickets.isEmpty() -> _state.update { it.copy(loadError = UiText.Resource(R.string.support_load_failed)) }
                        else -> _events.send(SupportEvent.ShowMessage(UiText.Resource(R.string.support_load_failed)))
                    }
                }
            }
        }
    }
}
