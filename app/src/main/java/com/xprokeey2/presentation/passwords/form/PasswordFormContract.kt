package com.xprokeey2.presentation.passwords.form

import com.xprokeey2.domain.model.PasswordStrength
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Default category of a new password, as in the web form. */
const val DEFAULT_CATEGORY = "Personal"

/** Add new password, or "Update Vault Details" when [isEditing]. */
data class PasswordFormUiState(
    val user: UserBadge? = null,
    val isEditing: Boolean = false,
    val isLoadingItem: Boolean = false,
    /** Edit only: the item couldn't be loaded. */
    val loadError: UiText? = null,
    val title: String = "",
    val url: String = "",
    val username: String = "",
    val category: String = DEFAULT_CATEGORY,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    /** Null while the password is empty (the meter is hidden). */
    val strength: PasswordStrength? = null,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val categories: List<String> = emptyList(),
    val titleError: UiText? = null,
    val urlError: UiText? = null,
    val usernameError: UiText? = null,
    val passwordError: UiText? = null,
    /** Edit only: the stored password isn't ciphertext under this vault key, so it must be re-entered. */
    val passwordUnreadable: Boolean = false,
    val isSaving: Boolean = false,
    val isNewCategoryDialogVisible: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryError: UiText? = null,
    val isCreatingCategory: Boolean = false,
)

sealed interface PasswordFormAction {
    data class TitleChanged(val value: String) : PasswordFormAction
    data class UrlChanged(val value: String) : PasswordFormAction
    data class UsernameChanged(val value: String) : PasswordFormAction
    data class CategorySelected(val category: String) : PasswordFormAction
    data class PasswordChanged(val value: String) : PasswordFormAction
    data object TogglePasswordVisibility : PasswordFormAction
    data object GeneratePassword : PasswordFormAction
    data class NotesChanged(val value: String) : PasswordFormAction
    data class FavoriteChanged(val isFavorite: Boolean) : PasswordFormAction
    data object NewCategoryClicked : PasswordFormAction
    data class NewCategoryNameChanged(val value: String) : PasswordFormAction
    data object NewCategoryConfirmed : PasswordFormAction
    data object NewCategoryDismissed : PasswordFormAction
    data object Save : PasswordFormAction
    data object RetryLoad : PasswordFormAction
}

sealed interface PasswordFormEvent {
    data class Saved(val message: UiText) : PasswordFormEvent
    data class ShowMessage(val message: UiText) : PasswordFormEvent

    /** Session over or vault locked: back to Login. */
    data class SignInRequired(val message: UiText) : PasswordFormEvent
}
