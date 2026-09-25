package com.xprokeey2.presentation.auth.verify

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.xprokeey2.domain.usecase.auth.ConfirmRecoveryKeySavedUseCase
import com.xprokeey2.domain.usecase.auth.ResendSignupOtpUseCase
import com.xprokeey2.domain.usecase.auth.VerifyAccountUseCase
import com.xprokeey2.domain.usecase.validation.ValidateOtpUseCase
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.navigation.VerifyEmailRoute
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyAccount: VerifyAccountUseCase,
    private val resendSignupOtp: ResendSignupOtpUseCase,
    private val validateOtp: ValidateOtpUseCase,
    private val confirmRecoveryKeySaved: ConfirmRecoveryKeySavedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        VerifyEmailUiState(email = savedStateHandle.toRoute<VerifyEmailRoute>().email)
    )
    val state = _state.asStateFlow()

    private val _events = Channel<VerifyEmailEvent>()
    val events = _events.receiveAsFlow()

    private var countdownJob: Job? = null

    init {
        // An OTP was just emailed (signup) or is still valid (login redirect): either way, wait before resending.
        startResendCountdown()
    }

    fun onAction(action: VerifyEmailAction) {
        when (action) {
            is VerifyEmailAction.OtpChanged -> _state.update { it.copy(otp = action.otp, otpError = null) }
            VerifyEmailAction.Verify -> verify()
            VerifyEmailAction.Resend -> resend()
            VerifyEmailAction.RecoveryKeySaved -> viewModelScope.launch {
                confirmRecoveryKeySaved(_state.value.email)
                _state.update { it.copy(recoveryKey = null) }
                _events.send(VerifyEmailEvent.NavigateToLogin(_state.value.email))
            }
        }
    }

    private fun verify() {
        val current = _state.value
        if (current.isVerifying) return

        validateOtp(current.otp)?.let { error ->
            _state.update { it.copy(otpError = error.asUiText()) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true) }
            val result = verifyAccount(email = current.email, otp = current.otp)
            _state.update { it.copy(isVerifying = false) }

            when (result) {
                is Resource.Success -> {
                    val recoveryKey = result.data.recoveryKey
                    if (recoveryKey != null) {
                        _state.update { it.copy(recoveryKey = recoveryKey) }
                    } else {
                        // Verified elsewhere / no key stored on this device: straight to login.
                        _events.send(VerifyEmailEvent.NavigateToLogin(current.email))
                    }
                }
                is Resource.Error -> _state.update { it.copy(otpError = result.error.asUiText()) }
            }
        }
    }

    private fun resend() {
        if (!_state.value.canResend) return

        viewModelScope.launch {
            _state.update { it.copy(isResending = true) }
            val result = resendSignupOtp(_state.value.email)
            _state.update { it.copy(isResending = false) }

            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(otp = "", otpError = null) }
                    startResendCountdown()
                    _events.send(VerifyEmailEvent.ShowMessage(UiText.Dynamic(result.data)))
                }
                is Resource.Error -> _events.send(VerifyEmailEvent.ShowMessage(result.error.asUiText()))
            }
        }
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in RESEND_COOLDOWN_SECONDS downTo 0) {
                _state.update { it.copy(resendSecondsLeft = seconds) }
                if (seconds > 0) delay(1_000)
            }
        }
    }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
