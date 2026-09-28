package com.xprokeey2.presentation.cards.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.usecase.card.DeleteCardUseCase
import com.xprokeey2.domain.usecase.card.GetCardDetailsUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.cards.components.formatExpiry
import com.xprokeey2.presentation.cards.components.labelRes
import com.xprokeey2.presentation.navigation.CardDetailsRoute
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
class CardDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCardDetails: GetCardDetailsUseCase,
    private val deleteCard: DeleteCardUseCase,
) : ViewModel() {

    private val cardId = savedStateHandle.toRoute<CardDetailsRoute>().cardId

    private val _state = MutableStateFlow(CardDetailsUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<CardDetailsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: CardDetailsAction) {
        when (action) {
            CardDetailsAction.Refresh -> load()
            CardDetailsAction.ToggleNumber -> toggleSecret(isNumber = true)
            CardDetailsAction.ToggleCvc -> toggleSecret(isNumber = false)
            is CardDetailsAction.Copy -> copy(action.field)
            CardDetailsAction.DeleteClicked -> _state.update { it.copy(isDeleteDialogVisible = true) }
            CardDetailsAction.DeleteDismissed -> _state.update { it.copy(isDeleteDialogVisible = false) }
            CardDetailsAction.DeleteConfirmed -> confirmDelete()
        }
    }

    private fun load() {
        viewModelScope.launch {
            // Secrets are hidden again whenever the screen comes back.
            _state.update {
                it.copy(isLoading = it.details == null, loadError = null, isNumberRevealed = false, isCvcRevealed = false)
            }
            val result = getCardDetails(cardId)
            _state.update { it.copy(isLoading = false) }
            when (result) {
                is Resource.Success -> _state.update { it.copy(details = result.data) }
                is Resource.Error -> handleError(result.error) { message ->
                    if (_state.value.details == null) {
                        _state.update { it.copy(loadError = message) }
                    } else {
                        _events.send(CardDetailsEvent.ShowMessage(message))
                    }
                }
            }
        }
    }

    private fun toggleSecret(isNumber: Boolean) {
        val details = _state.value.details ?: return
        val secret = if (isNumber) details.number else details.cvc
        if (secret == null) {
            viewModelScope.launch { _events.send(CardDetailsEvent.ShowMessage(DecryptFailed)) }
            return
        }
        _state.update {
            if (isNumber) it.copy(isNumberRevealed = !it.isNumberRevealed) else it.copy(isCvcRevealed = !it.isCvcRevealed)
        }
    }

    private fun copy(field: CardField) {
        val details = _state.value.details ?: return
        val card = details.card
        val (label, value) = when (field) {
            CardField.LABEL -> R.string.label_card_type to card.label.asText()
            CardField.HOLDER_NAME -> R.string.label_cardholder_name to card.holderName.asText()
            CardField.NUMBER -> R.string.label_card_number to details.number?.asText()
            CardField.BRAND -> R.string.label_brand to card.brand.displayName.asText()
            CardField.EXPIRY -> R.string.label_expiry_date to formatExpiry(card.expiryMonth, card.expiryYear).asText()
            CardField.CVC -> R.string.label_cvc to details.cvc?.asText()
            CardField.CATEGORY -> R.string.label_card_category to (
                CardCategory.fromCardType(card.category)?.let { UiText.Resource(it.labelRes) } ?: card.category.asText()
            )
            CardField.BANK_NAME -> R.string.label_bank_name to card.bankName.asText()
            CardField.NOTES -> R.string.label_notes to card.notes.asText()
        }
        viewModelScope.launch {
            if (value == null) {
                _events.send(CardDetailsEvent.ShowMessage(DecryptFailed))
            } else {
                _events.send(
                    CardDetailsEvent.CopyToClipboard(
                        label = UiText.Resource(label),
                        value = value,
                        sensitive = field == CardField.NUMBER || field == CardField.CVC,
                    )
                )
            }
        }
    }

    private fun confirmDelete() {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            val result = deleteCard(cardId)
            _state.update { it.copy(isDeleting = false, isDeleteDialogVisible = false) }
            when (result) {
                is Resource.Success -> _events.send(CardDetailsEvent.Deleted(UiText.Dynamic(result.data)))
                is Resource.Error -> handleError(result.error) { _events.send(CardDetailsEvent.ShowMessage(it)) }
            }
        }
    }

    private suspend fun handleError(error: DataError, show: suspend (UiText) -> Unit) {
        when (error) {
            DataError.SessionExpired, DataError.VaultLocked ->
                _events.send(CardDetailsEvent.SignInRequired(error.asUiText()))
            else -> show(error.asUiText())
        }
    }

    private fun String.asText(): UiText = UiText.Dynamic(this)

    private companion object {
        val DecryptFailed = UiText.Resource(R.string.card_decrypt_failed)
    }
}
