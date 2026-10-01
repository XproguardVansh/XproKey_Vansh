package com.xprokeey2.presentation.cards.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.model.Card
import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.domain.model.CardChanges
import com.xprokeey2.domain.model.CardDraft
import com.xprokeey2.domain.model.CardExpiry
import com.xprokeey2.domain.usecase.card.AddCardUseCase
import com.xprokeey2.domain.usecase.card.GetCardDetailsUseCase
import com.xprokeey2.domain.usecase.card.UpdateCardUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.validation.CardFormField
import com.xprokeey2.domain.usecase.validation.ValidateCardFormUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.CardFormRoute
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

/** Input limits from the card validation spec (§4, §5.1). */
private const val MAX_NUMBER_LENGTH = 16
private const val EXPIRY_LENGTH = 4
private const val MAX_TEXT_LENGTH = 100
private const val MAX_NOTES_LENGTH = 500

/** Web filter for the cardholder name: Latin letters, spaces, apostrophe and hyphen. */
private val HolderNameFilter = Regex("[^a-zA-Z\\s'-]")

@HiltViewModel
class CardFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSignedInUser: GetSignedInUserUseCase,
    private val getCardDetails: GetCardDetailsUseCase,
    private val addCard: AddCardUseCase,
    private val updateCard: UpdateCardUseCase,
    private val validateCardForm: ValidateCardFormUseCase,
) : ViewModel() {

    private val cardId: Long? = savedStateHandle.toRoute<CardFormRoute>().cardId

    /** Edit: the form as loaded, to send only what changed. */
    private var original: CardFormUiState? = null

    private val _state = MutableStateFlow(CardFormUiState(isEditing = cardId != null))
    val state = _state.asStateFlow()

    private val _events = Channel<CardFormEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()?.toBadge()
            _state.update { it.copy(user = user) }
        }
        if (cardId != null) loadCard(cardId)
    }

    fun onAction(action: CardFormAction) {
        when (action) {
            is CardFormAction.LabelChanged -> _state.update {
                it.copy(label = action.value.take(MAX_TEXT_LENGTH)).withoutErrors()
            }
            is CardFormAction.HolderNameChanged -> _state.update {
                it.copy(holderName = action.value.replace(HolderNameFilter, "").take(MAX_TEXT_LENGTH))
            }
            is CardFormAction.NumberChanged -> _state.update {
                val number = action.value.digits(MAX_NUMBER_LENGTH)
                it.copy(number = number, brand = CardBrand.detectOrKeep(number, it.brand)).withoutErrors()
            }
            is CardFormAction.ExpiryChanged -> _state.update {
                it.copy(expiry = action.value.digits(EXPIRY_LENGTH)).withoutErrors()
            }
            is CardFormAction.CvcChanged -> _state.update {
                it.copy(cvc = action.value.digits(it.brand.cvcLength)).withoutErrors()
            }
            CardFormAction.ToggleCvcVisibility -> _state.update { it.copy(isCvcVisible = !it.isCvcVisible) }
            is CardFormAction.CategorySelected -> _state.update { it.copy(category = action.category) }
            is CardFormAction.BankNameChanged -> _state.update { it.copy(bankName = action.value.take(MAX_TEXT_LENGTH)) }
            is CardFormAction.NotesChanged -> _state.update { it.copy(notes = action.value.take(MAX_NOTES_LENGTH)) }
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
                    val number = details.number.orEmpty().digits(MAX_NUMBER_LENGTH)
                    val loaded = _state.value.copy(
                        label = card.label,
                        holderName = card.holderName,
                        number = number,
                        brand = CardBrand.detectOrKeep(number, card.brand),
                        expiry = card.expiryDigits(),
                        cvc = details.cvc.orEmpty().digits(4),
                        category = CardCategory.fromCardType(card.category) ?: CardCategory.CREDIT,
                        bankName = card.bankName,
                        notes = card.notes,
                        secretsUnreadable = details.number == null || details.cvc == null,
                    )
                    original = loaded
                    _state.value = loaded
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

        val before = original
        // Edit: Luhn only runs when the number was changed (spec §3).
        val numberChanged = before == null || current.number != before.number || current.secretsUnreadable
        val error = validateCardForm(
            label = current.label,
            number = current.number,
            expiry = current.expiry,
            cvc = current.cvc,
            brand = current.brand,
            checkNumber = numberChanged,
            checkNotExpired = before == null || current.expiry != before.expiry,
        )
        if (error != null) {
            val message = error.error.asUiText()
            _state.update {
                it.copy(
                    labelError = message.takeIf { error.field == CardFormField.LABEL },
                    numberError = message.takeIf { error.field == CardFormField.NUMBER },
                    expiryError = message.takeIf { error.field == CardFormField.EXPIRY },
                    cvcError = message.takeIf { error.field == CardFormField.CVC },
                )
            }
            return
        }
        // Validation guarantees a complete, valid "MMYY".
        val expiry = CardExpiry.parse(current.expiry) ?: return

        if (cardId == null || before == null) {
            submit(isEdit = false) {
                addCard(
                    CardDraft(
                        label = current.label,
                        holderName = current.holderName,
                        number = current.number,
                        cvc = current.cvc,
                        expiry = expiry,
                        category = current.category,
                        brand = current.brand,
                        bankName = current.bankName,
                        notes = current.notes,
                    )
                )
            }
            return
        }

        val changes = changesSince(before, current, expiry)
        if (changes.isEmpty) {
            viewModelScope.launch { _events.send(CardFormEvent.ShowMessage(UiText.Resource(R.string.error_no_card_changes))) }
            return
        }
        submit(isEdit = true) { updateCard(cardId, changes) }
    }

    /** Only what differs from the loaded card; secrets that couldn't be read count as changed. */
    private fun changesSince(before: CardFormUiState, now: CardFormUiState, expiry: CardExpiry): CardChanges {
        val numberChanged = now.number != before.number || now.secretsUnreadable
        return CardChanges(
            label = now.label.takeIf { it.trim() != before.label.trim() },
            holderName = now.holderName.takeIf { it.trim() != before.holderName.trim() },
            number = now.number.takeIf { numberChanged },
            brand = now.brand.takeIf { numberChanged },
            cvc = now.cvc.takeIf { it != before.cvc || now.secretsUnreadable },
            expiry = expiry.takeIf { now.expiry != before.expiry },
            category = now.category.takeIf { it.cardType != before.category.cardType },
            bankName = now.bankName.takeIf { it.trim() != before.bankName.trim() },
            notes = now.notes.takeIf { it.trim() != before.notes.trim() },
        )
    }

    private fun submit(isEdit: Boolean, call: suspend () -> Resource<Card>) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = call()
            _state.update { it.copy(isSaving = false) }
            when (result) {
                is Resource.Success -> _events.send(
                    CardFormEvent.Saved(UiText.Resource(if (isEdit) R.string.card_updated else R.string.card_saved))
                )
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired, DataError.VaultLocked ->
                        _events.send(CardFormEvent.SignInRequired(error.asUiText()))
                    // e.g. "Invalid expiry: card already expired ..." from the server.
                    else -> _events.send(CardFormEvent.ShowMessage(error.asUiText()))
                }
            }
        }
    }

    private fun CardFormUiState.withoutErrors() =
        copy(labelError = null, numberError = null, expiryError = null, cvcError = null)

    private fun Card.expiryDigits(): String =
        if (expiryMonth in 1..12) "%02d%02d".format(expiryMonth, expiryYear % 100) else ""

    private fun String.digits(maxLength: Int): String = filter(Char::isDigit).take(maxLength)
}
