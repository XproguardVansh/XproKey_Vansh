package com.xprokeey2.presentation.settings.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.auth.ChangePasswordUseCase
import com.xprokeey2.domain.usecase.auth.RequestPasswordResetUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.validation.ChangePasswordFormError
import com.xprokeey2.domain.usecase.validation.ValidateChangePasswordFormUseCase
import com.xprokeey2.domain.usecase.validation.ValidateOtpUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val requestOtp: RequestPasswordResetUseCase,
    private val validateForm: ValidateChangePasswordFormUseCase,
    private val changePassword: ChangePasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<ChangePasswordEvent>()
    val events = _events.receiveAsFlow()

    init {
        // The OTP goes to the signed-in account's email.
        viewModelScope.launch {
            val user = getSignedInUser() ?: return@launch
            _state.update { it.copy(user = user.toBadge(), email = user.email) }
        }
    }

    fun onAction(action: ChangePasswordAction) {
        when (action) {
            ChangePasswordAction.SendOtp -> sendOtp()
            is ChangePasswordAction.OtpChanged -> _state.update {
                it.copy(otp = action.otp.filter(Char::isDigit).take(ValidateOtpUseCase.OTP_LENGTH), error = null)
            }
            is ChangePasswordAction.PasswordChanged -> _state.update { it.copy(password = action.password, error = null) }
            is ChangePasswordAction.ConfirmPasswordChanged -> _state.update {
                it.copy(confirmPassword = action.password, error = null)
            }
            ChangePasswordAction.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            ChangePasswordAction.ToggleConfirmPasswordVisibility -> _state.update {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            ChangePasswordAction.Back -> _state.update {
                it.copy(isOtpSent = false, otp = "", password = "", confirmPassword = "", error = null)
            }
            ChangePasswordAction.Submit -> submit()
        }
    }

    private fun sendOtp() {
        val current = _state.value
        if (current.isSendingOtp) return
        _state.update { it.copy(error = null) }
        if (current.email.isBlank()) {
            showError(UiText.Resource(R.string.change_password_email_missing))
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSendingOtp = true) }
            val result = requestOtp(current.email)
            _state.update { it.copy(isSendingOtp = false) }
            when (result) {
                is Resource.Success -> {
                    _state.update { it.copy(isOtpSent = true) }
                    // `res.message || "OTP has been sent to your email."`
                    val message = result.data.message.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
                        ?: UiText.Resource(R.string.change_password_otp_sent)
                    _events.send(ChangePasswordEvent.ShowMessage(message))
                }
                is Resource.Error -> showError(result.error.toChangePasswordMessage())
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        _state.update { it.copy(error = null) }

        val formError = validateForm(current.otp, current.password, current.confirmPassword)
        if (formError != null) {
            showError(formError.asUiText())
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = changePassword(
                email = current.email,
                otp = current.otp,
                newPassword = current.password,
                confirmPassword = current.confirmPassword,
            )
            _state.update { it.copy(isSubmitting = false) }
            when (result) {
                // `res.message || "Password changed successfully."`
                is Resource.Success -> _events.send(
                    ChangePasswordEvent.PasswordChanged(
                        result.data.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
                            ?: UiText.Resource(R.string.change_password_success)
                    )
                )
                is Resource.Error -> when (result.error) {
                    DataError.VaultLocked, DataError.SessionExpired -> _events.send(
                        ChangePasswordEvent.SignInRequired(UiText.Resource(R.string.change_password_vault_locked))
                    )
                    else -> showError(result.error.toChangePasswordMessage())
                }
            }
        }
    }

    /** Like the web: the message goes in the error box and in a toast (a snackbar here). */
    private fun showError(message: UiText) {
        _state.update { it.copy(error = message) }
        viewModelScope.launch { _events.send(ChangePasswordEvent.ShowMessage(message)) }
    }
}

private fun ChangePasswordFormError.asUiText(): UiText = UiText.Resource(
    when (this) {
        ChangePasswordFormError.OTP_REQUIRED -> R.string.change_password_otp_required
        ChangePasswordFormError.OTP_WRONG_LENGTH -> R.string.change_password_otp_length
        ChangePasswordFormError.PASSWORD_REQUIRED -> R.string.change_password_password_required
        ChangePasswordFormError.PASSWORD_TOO_SHORT -> R.string.change_password_password_short
        ChangePasswordFormError.PASSWORD_MISMATCH -> R.string.change_password_mismatch
    }
)

/**
 * The web page's `getErrorMessage`: some server errors get a clearer text, any other is shown as
 * the server wrote it, and failures without one get "Password change failed".
 */
internal fun DataError.toChangePasswordMessage(): UiText = when (this) {
    DataError.NoInternet, DataError.Timeout -> asUiText()
    is DataError.Server -> serverErrorMessage(message)
    is DataError.AccountNotVerified -> serverErrorMessage(message)
    else -> UiText.Resource(R.string.change_password_failed)
}

private fun serverErrorMessage(text: String): UiText {
    val backendError = text.trim()
    val lowercase = backendError.lowercase()
    return when {
        backendError.isEmpty() -> UiText.Resource(R.string.change_password_failed)
        "otp has expired" in lowercase -> UiText.Resource(R.string.change_password_otp_expired)
        "invalid otp" in lowercase -> UiText.Resource(R.string.change_password_invalid_otp)
        "invalid email or otp" in lowercase -> UiText.Resource(R.string.change_password_invalid_email_or_otp)
        "do not match" in lowercase -> UiText.Resource(R.string.change_password_mismatch)
        else -> UiText.Dynamic(backendError)
    }
}
