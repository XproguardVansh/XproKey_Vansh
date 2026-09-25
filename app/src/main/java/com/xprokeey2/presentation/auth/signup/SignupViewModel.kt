package com.xprokeey2.presentation.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.auth.SignupUseCase
import com.xprokeey2.domain.usecase.validation.EvaluatePasswordStrengthUseCase
import com.xprokeey2.domain.usecase.validation.ValidateSignupFormUseCase
import com.xprokeey2.domain.util.Resource
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
class SignupViewModel @Inject constructor(
    private val signup: SignupUseCase,
    private val validateSignupForm: ValidateSignupFormUseCase,
    private val evaluatePasswordStrength: EvaluatePasswordStrengthUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignupUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<SignupEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: SignupAction) {
        when (action) {
            is SignupAction.NameChanged -> _state.update {
                it.copy(name = action.name, nameError = null)
            }
            is SignupAction.EmailChanged -> _state.update {
                it.copy(email = action.email, emailError = null)
            }
            is SignupAction.PasswordChanged -> _state.update {
                it.copy(
                    password = action.password,
                    passwordStrength = if (action.password.isEmpty()) null else evaluatePasswordStrength(action.password),
                    passwordError = null,
                )
            }
            is SignupAction.ConfirmPasswordChanged -> _state.update {
                it.copy(confirmPassword = action.confirmPassword, confirmPasswordError = null)
            }
            SignupAction.TogglePasswordVisibility -> _state.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            SignupAction.ToggleConfirmPasswordVisibility -> _state.update {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            is SignupAction.AcceptedTermsChanged -> _state.update {
                it.copy(acceptedTerms = action.accepted, termsError = null)
            }
            SignupAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return

        val errors = validateSignupForm(
            name = current.name,
            email = current.email,
            password = current.password,
            confirmPassword = current.confirmPassword,
            acceptedTerms = current.acceptedTerms,
        )
        if (errors.hasErrors) {
            _state.update {
                it.copy(
                    nameError = errors.name?.asUiText(),
                    emailError = errors.email?.asUiText(),
                    passwordError = errors.password?.asUiText(),
                    confirmPasswordError = errors.confirmPassword?.asUiText(),
                    termsError = errors.terms?.asUiText(),
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = signup(
                name = current.name,
                email = current.email,
                password = current.password,
                confirmPassword = current.confirmPassword,
            )
            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> _events.send(SignupEvent.NavigateToVerify(result.data.user.email))
                is Resource.Error -> _events.send(SignupEvent.ShowMessage(result.error.asUiText()))
            }
        }
    }
}
