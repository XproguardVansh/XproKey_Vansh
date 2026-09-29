package com.xprokeey2.presentation.passwords.list

import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** The web list filter: All Items, Favorites, Weak Items, or one category. */
sealed interface PasswordFilter {
    data object All : PasswordFilter
    data object Favorites : PasswordFilter
    data object Weak : PasswordFilter
    data class Category(val name: String) : PasswordFilter
}

data class PasswordsUiState(
    val user: UserBadge? = null,
    /** First load only; later refreshes keep the list on screen. */
    val isLoading: Boolean = true,
    val items: List<VaultItem> = emptyList(),
    val loadError: UiText? = null,
    val categories: List<String> = emptyList(),
    val query: String = "",
    val filter: PasswordFilter = PasswordFilter.All,
    /** Same rule as the dashboard's weak items: no username, no URL, or marked weak. */
    val weakItemIds: Set<Long> = emptySet(),
    /** Items whose star is being saved. */
    val favoriteUpdatingIds: Set<Long> = emptySet(),
    val selectedIds: Set<Long> = emptySet(),
    val isDeleteDialogVisible: Boolean = false,
    val isDeleting: Boolean = false,
) {
    val visibleItems: List<VaultItem>
        get() {
            val filtered = when (filter) {
                PasswordFilter.All -> items
                PasswordFilter.Favorites -> items.filter { it.isFavorite }
                PasswordFilter.Weak -> items.filter { it.id in weakItemIds }
                is PasswordFilter.Category -> items.filter { it.category.equals(filter.name, ignoreCase = true) }
            }
            val needle = query.trim()
            if (needle.isEmpty()) return filtered
            return filtered.filter { item ->
                listOf(item.title, item.username, item.url, item.category).any { it.contains(needle, ignoreCase = true) }
            }
        }
}

sealed interface PasswordsAction {
    data object Refresh : PasswordsAction
    data class QueryChanged(val query: String) : PasswordsAction
    data class FilterSelected(val filter: PasswordFilter) : PasswordsAction
    data class ToggleSelected(val itemId: Long) : PasswordsAction
    data object ToggleSelectAll : PasswordsAction
    data object ClearSelection : PasswordsAction
    data class ToggleFavorite(val itemId: Long) : PasswordsAction
    data class CopyUsername(val item: VaultItem) : PasswordsAction
    data object DeleteClicked : PasswordsAction
    data object DeleteDismissed : PasswordsAction
    data object DeleteConfirmed : PasswordsAction
}

sealed interface PasswordsEvent {
    data class ShowMessage(val message: UiText) : PasswordsEvent
    data class CopyToClipboard(val label: UiText, val value: String) : PasswordsEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : PasswordsEvent
}
