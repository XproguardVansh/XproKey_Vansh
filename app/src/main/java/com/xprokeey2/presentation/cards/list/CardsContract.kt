package com.xprokeey2.presentation.cards.list

import com.xprokeey2.domain.model.Card
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

data class CardsUiState(
    val user: UserBadge? = null,
    /** First load only; later refreshes keep the list on screen. */
    val isLoading: Boolean = true,
    val cards: List<Card> = emptyList(),
    /** Shown instead of the list when nothing could be loaded. */
    val loadError: UiText? = null,
    val query: String = "",
    /** Full numbers of the cards the user revealed, formatted for the card face. */
    val revealedNumbers: Map<Long, String> = emptyMap(),
    val revealingCardId: Long? = null,
    val cardPendingDelete: Card? = null,
    val isDeleting: Boolean = false,
) {
    val visibleCards: List<Card>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return cards
            return cards.filter { card ->
                listOf(card.label, card.holderName, card.bankName, card.last4, card.brand.displayName)
                    .any { it.contains(needle, ignoreCase = true) }
            }
        }
}

sealed interface CardsAction {
    data object Refresh : CardsAction
    data class QueryChanged(val query: String) : CardsAction
    data class ToggleReveal(val cardId: Long) : CardsAction
    data class DeleteClicked(val card: Card) : CardsAction
    data object DeleteDismissed : CardsAction
    data object DeleteConfirmed : CardsAction
}

sealed interface CardsEvent {
    data class ShowMessage(val message: UiText) : CardsEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : CardsEvent
}
