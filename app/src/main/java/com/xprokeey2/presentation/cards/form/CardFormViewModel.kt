package com.xprokeey2.presentation.cards.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardDraft
import com.xprokeey2.domain.model.CardExpiry
import com.xprokeey2.domain.usecase.card.GetCardDetailsUseCase
import com.xprokeey2.domain.usecase.card.SaveCardUseCase
import com.xprokeey2.domain.usecase.validation.ValidateCardFormUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.CardFormRoute
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
class CardFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCardDetails: GetCardDetailsUseCase,
    private val saveCard: SaveCardUseCase,
    private val validateCardForm: ValidateCardFormUseCase,
) : ViewModel() {

    private val cardId: Long? = savedStateHandle.toRoute<CardFormRoute>().cardId

    private val _state = MutableStateFlow(CardFormUiState(isEditing = cardId != null))
    val state = _state.asStateFlow()

    private val _events = Channel<CardFormEvent>()
    val events = _events.receiveAsFlow()

    init {
        if (cardId != null) loadCard(cardId)
    }

    fun onAction(action: CardFormAction) {
        when (action) {
            is CardFormAction.LabelChanged -> _state.update { it.copy(label = action.value, labelError = null) }
            is CardFormAction.HolderNameChanged -> _state.update { it.copy(holderName = action.value) }
            is CardFormAction.NumberChanged -> _state.update {
                it.copy(number = action.value.digits(MAX_NUMBER_LENGTH), numberError = null)
            }
            is CardFormAction.ExpiryChanged -> _state.update {
                it.copy(expiry = action.value.digits(EXPIRY_LENGTH), expiryError = null)
            }
            is CardFormAction.CvcChanged -> _state.update {
                it.copy(cvc = action.value.digits(MAX_CVC_LENGTH), cvcError = null)
            }
            CardFormAction.ToggleCvcVisibility -> _state.update { it.copy(isCvcVisible = !it.isCvcVisible) }
            is CardFormAction.CategorySelected -> _state.update { it.copy(category = action.category) }
            is CardFormAction.BankNameChanged -> _state.update { it.copy(bankName = action.value) }
            is CardFormAction.NotesChanged -> _state.update { it.copy(notes = action.value) }
            CardFormAction.Save -> save()
            CardFormAction.RetryLoad -> cardId?.let(::loadCard)
        }
    }

    private fun loadCard(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingCard = true, loadError = null) }
            val result = getCardDetails(id)
            _state.update { it.copy(isLoadingCard = false) }
            when (result) {
                is Resource.Success -> {
                    val details = result.data
                    val card = details.card
                    _state.update {
                        it.copy(
                            label = card.label,
                            holderName = card.holderName,
                            number = details.number.orEmpty().digits(MAX_NUMBER_LENGTH),
                            expiry = if (card.expiryMonth in 1..12) {
                                "%02d%02d".format(card.expiryMonth, card.expiryYear % 100)
                            } else {
                                ""
                            },
                            cvc = details.cvc.orEmpty().digits(MAX_CVC_LENGTH),
                            category = CardCategory.fromApiValue(card.category) ?: CardCategory.CREDIT,
                            bankName = card.bankName,
                            notes = card.notes,
                            secretsUnreadable = details.number == null || details.cvc == null,
                        )
                    }
                }
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked ->
                        _events.send(CardFormEvent.SignInRequired(error.asUiText()))
                    else -> _state.update { it.copy(loadError = error.asUiText()) }
                }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoadingCard) return

        val errors = validateCardForm(
            label = current.label,
            number = current.number,
            expiry = current.expiry,
            cvc = current.cvc,
        )
        val expiry = CardExpiry.parse(current.expiry)
        if (errors.hasErrors || expiry == null) {
            _state.update {
                it.copy(
                    labelError = errors.label?.asUiText(),
                    numberError = errors.number?.asUiText(),
                    expiryError = errors.expiry?.asUiText(),
                    cvcError = errors.cvc?.asUiText(),
                )
            }
            return
        }

        val draft = CardDraft(
            label = current.label,
            holderName = current.holderName,
            number = current.number,
            cvc = current.cvc,
            expiry = expiry,
            category = current.category,
            bankName = current.bankName,
            notes = current.notes,
        )
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = saveCard(draft, cardId)
            _state.update { it.copy(isSaving = false) }
            when (result) {
                is Resource.Success -> _events.send(
                    CardFormEvent.Saved(UiText.Resource(if (cardId == null) R.string.card_saved else R.string.card_updated))
                )
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked ->
                        _events.send(CardFormEvent.SignInRequired(error.asUiText()))
                    else -> _events.send(CardFormEvent.ShowMessage(error.asUiText()))
                }
            }
        }
    }

    private fun String.digits(maxLength: Int): String = filter(Char::isDigit).take(maxLength)

    private companion object {
        const val MAX_NUMBER_LENGTH = 19
        const val EXPIRY_LENGTH = 4
        const val MAX_CVC_LENGTH = 4
    }
}
