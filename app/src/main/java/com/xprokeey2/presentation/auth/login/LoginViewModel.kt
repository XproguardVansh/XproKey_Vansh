package com.xprokeey2.presentation.auth.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.auth.LoginUseCase
import com.xprokeey2.domain.usecase.validation.ValidateLoginFormUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.LoginRoute
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
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val login: LoginUseCase,
    private val validateLoginForm: ValidateLoginFormUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<LoginRoute>()

    private val _state = MutableStateFlow(LoginUiState(email = route.email.orEmpty()))
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    init {
        // e.g. "Password reset successfully" after coming back from the reset flow.
        route.message?.let { message ->
            viewModelScope.launch { _events.send(LoginEvent.ShowMessage(UiText.Dynamic(message))) }
        }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _state.update {
                it.copy(email = action.email, emailError = null)
            }
            is LoginAction.PasswordChanged -> _state.update {
                it.copy(password = action.password, passwordError = null)
            }
            LoginAction.TogglePasswordVisibility -> _state.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            is LoginAction.RememberMeChanged -> _state.update { it.copy(rememberMe = action.checked) }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return

        val errors = validateLoginForm(current.email, current.password)
        if (errors.hasErrors) {
            _state.update {
                it.copy(
                    emailError = errors.email?.asUiText(),
                    passwordError = errors.password?.asUiText(),
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = login(current.email, current.password)
            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> _events.send(
                    LoginEvent.LoginSuccess(
                        if (result.data.hasVaultKeys) {
                            UiText.Dynamic(result.data.message)
                        } else {
                            UiText.Resource(R.string.login_success_no_vault)
                        }
                    )
                )
                is Resource.Error -> when (val error = result.error) {
                    // Unverified accounts go back to OTP entry; the earlier OTP is still valid.
                    is DataError.AccountNotVerified ->
                        _events.send(LoginEvent.NavigateToVerify(current.email.trim()))
                    else -> _events.send(LoginEvent.ShowMessage(error.asUiText()))
                }
            }
        }
    }
}
