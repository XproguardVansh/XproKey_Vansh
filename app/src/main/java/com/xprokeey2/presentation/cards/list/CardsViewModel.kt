package com.xprokeey2.presentation.cards.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.card.DeleteCardUseCase
import com.xprokeey2.domain.usecase.card.GetCardDetailsUseCase
import com.xprokeey2.domain.usecase.card.GetCardsUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.cards.components.formatCardNumber
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
class CardsViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getCards: GetCardsUseCase,
    private val getCardDetails: GetCardDetailsUseCase,
    private val deleteCard: DeleteCardUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(CardsUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<CardsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: CardsAction) {
        when (action) {
            CardsAction.Refresh -> refresh()
            is CardsAction.QueryChanged -> _state.update { it.copy(query = action.query) }
            is CardsAction.ToggleReveal -> toggleReveal(action.cardId)
            is CardsAction.DeleteClicked -> _state.update { it.copy(cardPendingDelete = action.card) }
            CardsAction.DeleteDismissed -> _state.update { it.copy(cardPendingDelete = null) }
            CardsAction.DeleteConfirmed -> confirmDelete()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            // Revealed numbers don't survive leaving the screen.
            _state.update { it.copy(isLoading = it.cards.isEmpty(), revealedNumbers = emptyMap()) }
            val result = getCards()
            _state.update { it.copy(isLoading = false) }
            when (result) {
                is Resource.Success -> _state.update { it.copy(cards = result.data, loadError = null) }
                is Resource.Error -> handleError(result.error) { message ->
                    if (_state.value.cards.isEmpty()) {
                        _state.update { it.copy(loadError = message) }
                    } else {
                        _events.send(CardsEvent.ShowMessage(message))
                    }
                }
            }
        }
    }

    private fun toggleReveal(cardId: Long) {
        val current = _state.value
        if (current.revealingCardId != null) return
        if (cardId in current.revealedNumbers) {
            _state.update { it.copy(revealedNumbers = it.revealedNumbers - cardId) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(revealingCardId = cardId) }
            val result = getCardDetails(cardId)
            _state.update { it.copy(revealingCardId = null) }
            when (result) {
                is Resource.Success -> {
                    val number = result.data.number
                    if (number.isNullOrEmpty()) {
                        _events.send(CardsEvent.ShowMessage(UiText.Resource(R.string.card_decrypt_failed)))
                    } else {
                        val formatted = formatCardNumber(number, result.data.card.brand)
                        _state.update { it.copy(revealedNumbers = it.revealedNumbers + (cardId to formatted)) }
                    }
                }
                is Resource.Error -> handleError(result.error) { _events.send(CardsEvent.ShowMessage(it)) }
            }
        }
    }

    private fun confirmDelete() {
        val card = _state.value.cardPendingDelete ?: return
        if (_state.value.isDeleting) return

        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            val result = deleteCard(card.id)
            _state.update { it.copy(isDeleting = false, cardPendingDelete = null) }
            when (result) {
                is Resource.Success -> {
                    _state.update { state ->
                        state.copy(
                            cards = state.cards.filterNot { it.id == card.id },
                            revealedNumbers = state.revealedNumbers - card.id,
                        )
                    }
                    _events.send(CardsEvent.ShowMessage(UiText.Dynamic(result.data)))
                }
                is Resource.Error -> handleError(result.error) { _events.send(CardsEvent.ShowMessage(it)) }
            }
        }
    }

    private suspend fun handleError(error: DataError, show: suspend (UiText) -> Unit) {
        when (error) {
            DataError.SessionExpired, DataError.VaultLocked -> _events.send(CardsEvent.SignInRequired(error.asUiText()))
            else -> show(error.asUiText())
        }
    }
}
