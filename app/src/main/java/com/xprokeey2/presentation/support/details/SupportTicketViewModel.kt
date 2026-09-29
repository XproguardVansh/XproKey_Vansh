package com.xprokeey2.presentation.support.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.support.GetSupportTicketUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.SupportTicketRoute
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
class SupportTicketViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTicket: GetSupportTicketUseCase,
) : ViewModel() {

    private val ticketId = savedStateHandle.toRoute<SupportTicketRoute>().ticketId

    private val _state = MutableStateFlow(SupportTicketUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<SupportTicketEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: SupportTicketAction) {
        when (action) {
            SupportTicketAction.Refresh -> refresh()
            SupportTicketAction.CopyTicketId -> _state.value.ticket?.let { ticket ->
                viewModelScope.launch { _events.send(SupportTicketEvent.CopyToClipboard(ticket.displayId)) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = getTicket(ticketId)) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, ticket = result.data) }
                is Resource.Error -> {
                    val error = result.error
                    _events.send(
                        if (error == DataError.SessionExpired) {
                            SupportTicketEvent.SignInRequired(error.asUiText())
                        } else {
                            SupportTicketEvent.LoadFailed(error.serverTextOr(R.string.ticket_load_failed))
                        }
                    )
                }
            }
        }
    }

    /** The web's Refresh button: "Ticket refreshed." or "Failed to refresh ticket." */
    private fun refresh() {
        if (_state.value.isRefreshing) return
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val result = getTicket(ticketId)
            _state.update { it.copy(isRefreshing = false) }
            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(ticket = result.data) }
                    _events.send(SupportTicketEvent.ShowMessage(UiText.Resource(R.string.ticket_refreshed)))
                }
                is Resource.Error -> _events.send(
                    if (result.error == DataError.SessionExpired) {
                        SupportTicketEvent.SignInRequired(result.error.asUiText())
                    } else {
                        SupportTicketEvent.ShowMessage(UiText.Resource(R.string.ticket_refresh_failed))
                    }
                )
            }
        }
    }

    /** `err.response.data.error || … || fallback` */
    private fun DataError.serverTextOr(fallback: Int): UiText =
        (this as? DataError.Server)?.message?.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
            ?: UiText.Resource(fallback)
}
