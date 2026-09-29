package com.xprokeey2.presentation.passwords.form

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.model.VaultItemChanges
import com.xprokeey2.domain.model.VaultItemDraft
import com.xprokeey2.domain.usecase.validation.EvaluatePasswordStrengthUseCase
import com.xprokeey2.domain.usecase.validation.ValidateVaultItemFormUseCase
import com.xprokeey2.domain.usecase.validation.ValidationError
import com.xprokeey2.domain.usecase.vault.AddVaultItemUseCase
import com.xprokeey2.domain.usecase.vault.CreateVaultCategoryUseCase
import com.xprokeey2.domain.usecase.vault.GeneratePasswordUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultCategoriesUseCase
import com.xprokeey2.domain.usecase.vault.GetVaultItemDetailsUseCase
import com.xprokeey2.domain.usecase.vault.UpdateVaultItemUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.PasswordFormRoute
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
class PasswordFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItemDetails: GetVaultItemDetailsUseCase,
    private val getCategories: GetVaultCategoriesUseCase,
    private val createCategory: CreateVaultCategoryUseCase,
    private val addItem: AddVaultItemUseCase,
    private val updateItem: UpdateVaultItemUseCase,
    private val generatePassword: GeneratePasswordUseCase,
    private val evaluateStrength: EvaluatePasswordStrengthUseCase,
    private val validateForm: ValidateVaultItemFormUseCase,
) : ViewModel() {

    private val itemId: Long? = savedStateHandle.toRoute<PasswordFormRoute>().itemId

    /** Edit: the form as loaded, to send only what changed. */
    private var original: PasswordFormUiState? = null

    private val _state = MutableStateFlow(PasswordFormUiState(isEditing = itemId != null))
    val state = _state.asStateFlow()

    private val _events = Channel<PasswordFormEvent>()
    val events = _events.receiveAsFlow()

    init {
        loadCategories()
        if (itemId != null) loadItem(itemId)
    }

    fun onAction(action: PasswordFormAction) {
        when (action) {
            is PasswordFormAction.TitleChanged -> _state.update { it.copy(title = action.value, titleError = null) }
            is PasswordFormAction.UrlChanged -> _state.update { it.copy(url = action.value, urlError = null) }
            is PasswordFormAction.UsernameChanged -> _state.update { it.copy(username = action.value, usernameError = null) }
            is PasswordFormAction.CategorySelected -> _state.update { it.copy(category = action.category) }
            is PasswordFormAction.PasswordChanged -> _state.update { it.withPassword(action.value) }
            PasswordFormAction.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            // Like the web form: a 16-character password is filled in and shown.
            PasswordFormAction.GeneratePassword -> _state.update {
                it.withPassword(generatePassword()).copy(isPasswordVisible = true)
            }
            is PasswordFormAction.NotesChanged -> _state.update { it.copy(notes = action.value) }
            is PasswordFormAction.FavoriteChanged -> _state.update { it.copy(isFavorite = action.isFavorite) }
            PasswordFormAction.NewCategoryClicked -> _state.update {
                it.copy(isNewCategoryDialogVisible = true, newCategoryName = "", newCategoryError = null)
            }
            is PasswordFormAction.NewCategoryNameChanged -> _state.update {
                it.copy(newCategoryName = action.value, newCategoryError = null)
            }
            PasswordFormAction.NewCategoryConfirmed -> addCategory()
            PasswordFormAction.NewCategoryDismissed -> _state.update { it.copy(isNewCategoryDialogVisible = false) }
            PasswordFormAction.Save -> save()
            PasswordFormAction.RetryLoad -> itemId?.let(::loadItem)
        }
    }

    private fun PasswordFormUiState.withPassword(password: String) = copy(
        password = password,
        strength = password.takeIf { it.isNotEmpty() }?.let(evaluateStrength::invoke),
        passwordError = null,
    )

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = getCategories()) {
                is Resource.Success -> _state.update { it.copy(categories = result.data) }
                is Resource.Error -> handleError(result.error) { /* the dropdown still offers the current category */ }
            }
        }
    }

    private fun loadItem(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingItem = true, loadError = null) }
            val result = getItemDetails(id)
            _state.update { it.copy(isLoadingItem = false) }
            when (result) {
                is Resource.Success -> {
                    val item = result.data.item
                    val loaded = _state.value.copy(
                        title = item.title,
                        url = item.url,
                        username = item.username,
                        category = item.category.ifBlank { DEFAULT_CATEGORY },
                        notes = item.notes,
                        isFavorite = item.isFavorite,
                        passwordUnreadable = result.data.password == null,
                    ).withPassword(result.data.password.orEmpty())
                    original = loaded
                    _state.value = loaded
                }
                is Resource.Error -> handleError(result.error) { message -> _state.update { it.copy(loadError = message) } }
            }
        }
    }

    private fun addCategory() {
        val current = _state.value
        val name = current.newCategoryName.trim()
        if (current.isCreatingCategory) return
        if (name.isEmpty()) {
            _state.update { it.copy(newCategoryError = ValidationError.REQUIRED.asUiText()) }
            return
        }
        // Already exists: just pick it.
        current.categories.firstOrNull { it.equals(name, ignoreCase = true) }?.let { existing ->
            _state.update { it.copy(category = existing, isNewCategoryDialogVisible = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreatingCategory = true) }
            val result = createCategory(name)
            _state.update { it.copy(isCreatingCategory = false) }
            when (result) {
                is Resource.Success -> _state.update {
                    it.copy(
                        categories = it.categories + result.data,
                        category = result.data,
                        isNewCategoryDialogVisible = false,
                    )
                }
                is Resource.Error -> handleError(result.error) { message ->
                    _state.update { it.copy(newCategoryError = message) }
                }
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoadingItem) return

        val errors = validateForm(
            title = current.title,
            url = current.url,
            username = current.username,
            password = current.password,
        )
        if (errors.hasErrors) {
            _state.update {
                it.copy(
                    titleError = errors.title?.asUiText(),
                    urlError = errors.url?.asUiText(),
                    usernameError = errors.username?.asUiText(),
                    passwordError = errors.password?.asUiText(),
                )
            }
            return
        }

        val before = original
        if (itemId == null || before == null) {
            submit(R.string.password_saved) {
                addItem(
                    VaultItemDraft(
                        title = current.title,
                        username = current.username,
                        url = current.url,
                        password = current.password,
                        category = current.category,
                        notes = current.notes,
                        isFavorite = current.isFavorite,
                    )
                )
            }
            return
        }

        val changes = VaultItemChanges(
            title = current.title.takeIf { it.trim() != before.title.trim() },
            username = current.username.takeIf { it.trim() != before.username.trim() },
            url = current.url.takeIf { it.trim() != before.url.trim() },
            password = current.password.takeIf { it != before.password || current.passwordUnreadable },
            category = current.category.takeIf { it != before.category },
            notes = current.notes.takeIf { it.trim() != before.notes.trim() },
            isFavorite = current.isFavorite.takeIf { it != before.isFavorite },
        )
        if (changes.isEmpty) {
            viewModelScope.launch { _events.send(PasswordFormEvent.ShowMessage(UiText.Resource(R.string.error_no_card_changes))) }
            return
        }
        submit(R.string.password_updated) { updateItem(itemId, changes) }
    }

    private fun submit(@StringRes successMessage: Int, call: suspend () -> Resource<Any>) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = call()
            _state.update { it.copy(isSaving = false) }
            when (result) {
                is Resource.Success -> _events.send(PasswordFormEvent.Saved(UiText.Resource(successMessage)))
                is Resource.Error -> handleError(result.error) { _events.send(PasswordFormEvent.ShowMessage(it)) }
            }
        }
    }

    private suspend fun handleError(error: DataError, show: suspend (UiText) -> Unit) {
        when (error) {
            DataError.SessionExpired, DataError.VaultLocked ->
                _events.send(PasswordFormEvent.SignInRequired(error.asUiText()))
            else -> show(error.asUiText())
        }
    }
}
