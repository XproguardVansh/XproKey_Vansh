package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

/** Why "Create Your Master Password" (first Google sign-in) can't be sent. */
enum class VaultSetupFormError {
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
}

/** The web Google dialog's checks, in its order. */
class ValidateVaultSetupFormUseCase @Inject constructor() {

    /** Returns the first failed check, or null when the form can be sent. */
    operator fun invoke(password: String, confirmPassword: String): VaultSetupFormError? = when {
        password.length < ValidateNewPasswordUseCase.MIN_LENGTH -> VaultSetupFormError.PASSWORD_TOO_SHORT
        password != confirmPassword -> VaultSetupFormError.PASSWORD_MISMATCH
        else -> null
    }
}
