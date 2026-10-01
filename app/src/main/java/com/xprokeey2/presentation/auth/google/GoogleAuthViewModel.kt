package com.xprokeey2.presentation.auth.google

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.model.GoogleSignInStep
import com.xprokeey2.domain.usecase.auth.SetupGoogleVaultUseCase
import com.xprokeey2.domain.usecase.auth.SignInWithGoogleUseCase
import com.xprokeey2.domain.usecase.auth.UnlockGoogleVaultUseCase
import com.xprokeey2.domain.usecase.security.RecordLastActivityUseCase
import com.xprokeey2.domain.usecase.validation.ValidateVaultSetupFormUseCase
import com.xprokeey2.domain.usecase.validation.VaultSetupFormError
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
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

/**
 * The web's GoogleAuthButton, shared by Login and Sign up: the ID token goes to POST /auth/google,
 * then a new account creates its master password (and sees its recovery key), a returning one enters
 * its master password, and only then is the session saved and the app opened.
 */
@HiltViewModel
class GoogleAuthViewModel @Inject constructor(
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val setupVault: SetupGoogleVaultUseCase,
    private val unlockVault: UnlockGoogleVaultUseCase,
    private val validateSetupForm: ValidateVaultSetupFormUseCase,
    private val recordLastActivity: RecordLastActivityUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoogleAuthUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<GoogleAuthEvent>()
    val events = _events.receiveAsFlow()

    // Kept out of the UI state, in memory only: the web keeps them in component state as well.
    private var setupToken: String? = null
    private var pendingSession: GoogleSession? = null

    /** What Continue in the recovery-key dialog does. */
    private var afterRecoveryKey: GoogleAuthEvent? = null

    fun onAction(action: GoogleAuthAction) {
        when (action) {
            GoogleAuthAction.PickerOpened -> _state.update { it.copy(isAuthenticating = true) }
            is GoogleAuthAction.PickerClosed -> onPickerClosed(action.result)

            is GoogleAuthAction.SetupPasswordChanged -> updateSetup { it.copy(password = action.value) }
            is GoogleAuthAction.SetupConfirmPasswordChanged -> updateSetup { it.copy(confirmPassword = action.value) }
            GoogleAuthAction.ToggleSetupPasswordVisibility -> updateSetup { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            GoogleAuthAction.ToggleSetupConfirmPasswordVisibility -> updateSetup {
                it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible)
            }
            GoogleAuthAction.SubmitSetup -> submitSetup()
            GoogleAuthAction.DismissSetup -> if (_state.value.setup?.isSubmitting != true) {
                setupToken = null
                _state.update { it.copy(setup = null) }
            }

            is GoogleAuthAction.UnlockPasswordChanged -> updateUnlock { it.copy(password = action.value) }
            GoogleAuthAction.ToggleUnlockPasswordVisibility -> updateUnlock { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            GoogleAuthAction.SubmitUnlock -> submitUnlock()
            GoogleAuthAction.DismissUnlock -> if (_state.value.unlock?.isSubmitting != true) {
                pendingSession = null
                _state.update { it.copy(unlock = null) }
            }
            GoogleAuthAction.ForgotMasterPassword -> forgotMasterPassword()

            GoogleAuthAction.RecoveryKeySaved -> {
                _state.update { it.copy(recovery = null) }
                afterRecoveryKey?.let { event -> viewModelScope.launch { _events.send(event) } }
                afterRecoveryKey = null
            }
        }
    }

    private fun onPickerClosed(result: GoogleIdTokenResult) {
        when (result) {
            is GoogleIdTokenResult.Success -> signIn(result.idToken)
            GoogleIdTokenResult.Cancelled -> _state.update { it.copy(isAuthenticating = false) }
            GoogleIdTokenResult.Failed -> {
                _state.update { it.copy(isAuthenticating = false) }
                viewModelScope.launch { _events.send(GoogleAuthEvent.ShowMessage(UiText.Resource(R.string.google_auth_failed))) }
            }
        }
    }

    private fun signIn(idToken: String) {
        viewModelScope.launch {
            val result = signInWithGoogle(idToken)
            _state.update { it.copy(isAuthenticating = false) }
            when (result) {
                is Resource.Success -> when (val step = result.data) {
                    is GoogleSignInStep.CreateMasterPassword -> {
                        setupToken = step.setupToken
                        _state.update { it.copy(setup = VaultSetupDialogState(name = step.name, email = step.email)) }
                    }
                    is GoogleSignInStep.EnterMasterPassword -> {
                        pendingSession = step.session
                        _state.update { it.copy(unlock = VaultUnlockDialogState(email = step.session.user.email)) }
                    }
                    is GoogleSignInStep.SignedIn -> {
                        recordLastActivity(System.currentTimeMillis())
                        val message = step.message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic)
                            ?: UiText.Resource(R.string.google_sign_in_successful)
                        _events.send(GoogleAuthEvent.SignedIn(step.needsAccountSetup, message))
                    }
                }
                is Resource.Error -> _events.send(
                    GoogleAuthEvent.ShowMessage(result.error.messageOr(R.string.google_sign_in_failed))
                )
            }
        }
    }

    private fun submitSetup() {
        val setup = _state.value.setup ?: return
        val token = setupToken ?: return
        if (setup.isSubmitting) return

        val error = validateSetupForm(setup.password, setup.confirmPassword)
        if (error != null) {
            updateSetup { it.copy(error = UiText.Resource(error.messageRes())) }
            return
        }

        viewModelScope.launch {
            updateSetup { it.copy(isSubmitting = true, error = null) }
            val result = setupVault(
                setupToken = token,
                password = setup.password,
                confirmPassword = setup.confirmPassword,
                name = setup.name,
                email = setup.email,
            )
            when (result) {
                is Resource.Success -> {
                    setupToken = null
                    val vault = result.data
                    if (vault.isSignedIn) recordLastActivity(System.currentTimeMillis())
                    afterRecoveryKey = if (vault.isSignedIn) {
                        GoogleAuthEvent.SignedIn(needsAccountSetup = vault.needsAccountSetup, message = null)
                    } else {
                        GoogleAuthEvent.ShowMessage(UiText.Resource(R.string.google_sign_in_again))
                    }
                    _state.update {
                        it.copy(setup = null, recovery = RecoveryKeyDialogState(email = setup.email, recoveryKey = vault.recoveryKey))
                    }
                    _events.send(GoogleAuthEvent.ShowMessage(UiText.Resource(R.string.google_setup_done)))
                }
                is Resource.Error -> {
                    // Like the web: in the dialog and as a toast.
                    val message = result.error.messageOr(R.string.google_setup_failed)
                    updateSetup { it.copy(isSubmitting = false, error = message) }
                    _events.send(GoogleAuthEvent.ShowMessage(message))
                }
            }
        }
    }

    private fun submitUnlock() {
        val unlock = _state.value.unlock ?: return
        val session = pendingSession ?: return
        if (unlock.isSubmitting || unlock.password.isEmpty()) return

        viewModelScope.launch {
            updateUnlock { it.copy(isSubmitting = true, error = null) }
            if (unlockVault(session, unlock.password)) {
                pendingSession = null
                recordLastActivity(System.currentTimeMillis())
                _state.update { it.copy(unlock = null) }
                _events.send(
                    GoogleAuthEvent.SignedIn(
                        needsAccountSetup = session.needsAccountSetup,
                        message = UiText.Resource(R.string.google_vault_unlocked),
                    )
                )
            } else {
                updateUnlock { it.copy(isSubmitting = false, error = UiText.Resource(R.string.google_unlock_incorrect)) }
            }
        }
    }

    /** The web's link leaves the page, so the unlock dialog and its unsaved session go with it. */
    private fun forgotMasterPassword() {
        val unlock = _state.value.unlock ?: return
        if (unlock.isSubmitting) return
        pendingSession = null
        _state.update { it.copy(unlock = null) }
        viewModelScope.launch { _events.send(GoogleAuthEvent.OpenForgotPassword(unlock.email)) }
    }

    private fun updateSetup(transform: (VaultSetupDialogState) -> VaultSetupDialogState) {
        _state.update { state -> state.setup?.let { state.copy(setup = transform(it)) } ?: state }
    }

    private fun updateUnlock(transform: (VaultUnlockDialogState) -> VaultUnlockDialogState) {
        _state.update { state -> state.unlock?.let { state.copy(unlock = transform(it)) } ?: state }
    }
}

/**
 * The web shows the server's "error" (or "message"), else its own fallback. No connection keeps the
 * app's usual wording.
 */
private fun DataError.messageOr(@StringRes fallback: Int): UiText = when (this) {
    is DataError.Server -> message.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: UiText.Resource(fallback)
    DataError.NoInternet, DataError.Timeout -> asUiText()
    else -> UiText.Resource(fallback)
}

private fun VaultSetupFormError.messageRes(): Int = when (this) {
    VaultSetupFormError.PASSWORD_TOO_SHORT -> R.string.google_setup_password_too_short
    VaultSetupFormError.PASSWORD_MISMATCH -> R.string.google_setup_password_mismatch
}
