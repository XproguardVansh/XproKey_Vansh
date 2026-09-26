package com.xprokeey2.presentation.cards.form

import com.xprokeey2.domain.model.CardBrand
import com.xprokeey2.domain.model.CardCategory
import com.xprokeey2.presentation.util.UiText

/** Add card, or Edit card when [isEditing]. Number, expiry ("MMYY") and CVC hold digits only. */
data class CardFormUiState(
    val isEditing: Boolean = false,
    val isLoadingCard: Boolean = false,
    /** Edit only: the card couldn't be loaded. */
    val loadError: UiText? = null,
    val label: String = "",
    val holderName: String = "",
    val number: String = "",
    val expiry: String = "",
    val cvc: String = "",
    val isCvcVisible: Boolean = false,
    val category: CardCategory = CardCategory.CREDIT,
    val bankName: String = "",
    val notes: String = "",
    val labelError: UiText? = null,
    val numberError: UiText? = null,
    val expiryError: UiText? = null,
    val cvcError: UiText? = null,
    /** Edit only: the stored number/CVC aren't ciphertext under this vault key, so they must be re-entered. */
    val secretsUnreadable: Boolean = false,
    val isSaving: Boolean = false,
) {
    val brand: CardBrand get() = CardBrand.detect(number)
}

sealed interface CardFormAction {
    data class LabelChanged(val value: String) : CardFormAction
    data class HolderNameChanged(val value: String) : CardFormAction
    data class NumberChanged(val value: String) : CardFormAction
    data class ExpiryChanged(val value: String) : CardFormAction
    data class CvcChanged(val value: String) : CardFormAction
    data object ToggleCvcVisibility : CardFormAction
    data class CategorySelected(val category: CardCategory) : CardFormAction
    data class BankNameChanged(val value: String) : CardFormAction
    data class NotesChanged(val value: String) : CardFormAction
    data object Save : CardFormAction
    data object RetryLoad : CardFormAction
}

sealed interface CardFormEvent {
    data class Saved(val message: UiText) : CardFormEvent
    data class ShowMessage(val message: UiText) : CardFormEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : CardFormEvent
}
