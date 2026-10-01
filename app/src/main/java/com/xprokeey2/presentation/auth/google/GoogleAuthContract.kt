package com.xprokeey2.presentation.auth.google

import com.xprokeey2.domain.model.MasterPasswordStrength
import com.xprokeey2.presentation.util.UiText

/** The web's Google button on Login and Sign up, with its three dialogs. */
data class GoogleAuthUiState(
    /** From opening Google's account picker until the server answered: the button shows it's busy. */
    val isAuthenticating: Boolean = false,
    val setup: VaultSetupDialogState? = null,
    val unlock: VaultUnlockDialogState? = null,
    val recovery: RecoveryKeyDialogState? = null,
)

/** "Create Your Master Password", for an account's first Google sign-in. */
data class VaultSetupDialogState(
    val name: String,
    val email: String,
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val error: UiText? = null,
    val isSubmitting: Boolean = false,
) {
    val strength: MasterPasswordStrength get() = MasterPasswordStrength.of(password)

    /** Like the web's button: both passwords typed and the same. */
    val canSubmit: Boolean get() = password.isNotEmpty() && password == confirmPassword
}

/** "Enter Master Password", for a returning Google user. */
data class VaultUnlockDialogState(
    val email: String,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val error: UiText? = null,
    val isSubmitting: Boolean = false,
)

/** "Save your recovery key", right after the master password was created. */
data class RecoveryKeyDialogState(
    val email: String,
    val recoveryKey: String,
)

sealed interface GoogleAuthAction {
    data object PickerOpened : GoogleAuthAction
    data class PickerClosed(val result: GoogleIdTokenResult) : GoogleAuthAction

    data class SetupPasswordChanged(val value: String) : GoogleAuthAction
    data class SetupConfirmPasswordChanged(val value: String) : GoogleAuthAction
    data object ToggleSetupPasswordVisibility : GoogleAuthAction
    data object ToggleSetupConfirmPasswordVisibility : GoogleAuthAction
    data object SubmitSetup : GoogleAuthAction
    data object DismissSetup : GoogleAuthAction

    data class UnlockPasswordChanged(val value: String) : GoogleAuthAction
    data object ToggleUnlockPasswordVisibility : GoogleAuthAction
    data object SubmitUnlock : GoogleAuthAction
    data object DismissUnlock : GoogleAuthAction
    data object ForgotMasterPassword : GoogleAuthAction

    data object RecoveryKeySaved : GoogleAuthAction
}

sealed interface GoogleAuthEvent {
    /** Signed in with the vault open: on to the same places as a password login. */
    data class SignedIn(val needsAccountSetup: Boolean, val message: UiText?) : GoogleAuthEvent

    data class ShowMessage(val message: UiText) : GoogleAuthEvent

    /** "Forgot master password?" in the unlock dialog. */
    data class OpenForgotPassword(val email: String) : GoogleAuthEvent
}
