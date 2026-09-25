package com.xprokeey2.presentation.auth.reset

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.domain.usecase.auth.ResetPasswordUseCase
import com.xprokeey2.domain.usecase.validation.ValidateOtpUseCase
import com.xprokeey2.domain.usecase.validation.ValidateResetPasswordFormUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.ResetPasswordRoute
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
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resetPassword: ResetPasswordUseCase,
    private val validateForm: ValidateResetPasswordFormUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ResetPasswordRoute>()

    private val _state = MutableStateFlow(
        ResetPasswordUiState(
            email = route.email,
            requiresRecoveryKey = route.encryptedVaultKeyRecovery.isNotBlank() && route.masterSalt.isNotBlank(),
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<ResetPasswordEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.OtpChanged -> _state.update {
                it.copy(
                    otp = action.otp.filter(Char::isDigit).take(ValidateOtpUseCase.OTP_LENGTH),
                    otpError = null,
                )
            }
            is ResetPasswordAction.RecoveryKeyChanged -> _state.update {
                it.copy(recoveryKey = action.recoveryKey, recoveryKeyError = null)
            }
            is ResetPasswordAction.NewPasswordChanged -> _state.update {
                it.copy(newPassword = action.password, newPasswordError = null)
            }
            is ResetPasswordAction.ConfirmPasswordChanged -> _state.update {
                it.copy(confirmPassword = action.password, confirmPasswordError = null)
            }
            ResetPasswordAction.ToggleNewPasswordVisibility -> _state.update {
                it.copy(isNewPasswordVisible = !it.isNewPasswordVisible)
            }
            ResetPasswordAction.ToggleConfirmPasswordVisibility -> _state.update {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            ResetPasswordAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return

        val errors = validateForm(
            otp = current.otp,
            recoveryKey = current.recoveryKey,
            newPassword = current.newPassword,
            confirmPassword = current.confirmPassword,
            recoveryKeyRequired = current.requiresRecoveryKey,
        )
        if (errors.hasErrors) {
            _state.update {
                it.copy(
                    otpError = errors.otp?.asUiText(),
                    recoveryKeyError = errors.recoveryKey?.asUiText(),
                    newPasswordError = errors.newPassword?.asUiText(),
                    confirmPasswordError = errors.confirmPassword?.asUiText(),
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = resetPassword(
                email = current.email,
                otp = current.otp,
                recoveryKey = current.recoveryKey,
                newPassword = current.newPassword,
                confirmPassword = current.confirmPassword,
                masterSalt = route.masterSalt,
                encryptedVaultKeyRecovery = route.encryptedVaultKeyRecovery,
            )
            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> _events.send(ResetPasswordEvent.PasswordReset(current.email, result.data))
                is Resource.Error -> when (result.error) {
                    DataError.InvalidRecoveryKey -> _state.update {
                        it.copy(recoveryKeyError = result.error.asUiText())
                    }
                    else -> _events.send(ResetPasswordEvent.ShowMessage(result.error.asUiText()))
                }
            }
        }
    }
}
