package com.xprokeey2.presentation.auth.forgot

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.domain.usecase.auth.RequestPasswordResetUseCase
import com.xprokeey2.domain.usecase.validation.ValidateEmailUseCase
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.ForgotPasswordRoute
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
class ForgotPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestPasswordReset: RequestPasswordResetUseCase,
    private val validateEmail: ValidateEmailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ForgotPasswordUiState(email = savedStateHandle.toRoute<ForgotPasswordRoute>().email)
    )
    val state = _state.asStateFlow()

    private val _events = Channel<ForgotPasswordEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ForgotPasswordAction) {
        when (action) {
            is ForgotPasswordAction.EmailChanged -> _state.update {
                it.copy(email = action.email, emailError = null)
            }
            ForgotPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return

        validateEmail(current.email)?.let { error ->
            _state.update { it.copy(emailError = error.asUiText()) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = requestPasswordReset(current.email)
            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> _events.send(
                    ForgotPasswordEvent.NavigateToReset(
                        email = current.email.trim(),
                        masterSalt = result.data.masterSalt,
                        encryptedVaultKeyRecovery = result.data.encryptedVaultKeyRecovery,
                    )
                )
                is Resource.Error -> _events.send(ForgotPasswordEvent.ShowMessage(result.error.asUiText()))
            }
        }
    }
}
