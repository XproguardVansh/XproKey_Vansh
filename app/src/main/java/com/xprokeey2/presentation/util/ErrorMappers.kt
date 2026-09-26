package com.xprokeey2.presentation.util

import com.xprokeey2.R
import com.xprokeey2.domain.usecase.validation.ValidationError
import com.xprokeey2.domain.util.DataError

fun DataError.asUiText(): UiText = when (this) {
    is DataError.Server -> UiText.Dynamic(message)
    is DataError.AccountNotVerified -> UiText.Dynamic(message)
    DataError.VaultUnlockFailed -> UiText.Resource(R.string.error_vault_unlock_failed)
    DataError.InvalidRecoveryKey -> UiText.Resource(R.string.error_invalid_recovery_key)
    DataError.SessionExpired -> UiText.Resource(R.string.error_session_expired)
    DataError.NoInternet -> UiText.Resource(R.string.error_no_internet)
    DataError.Timeout -> UiText.Resource(R.string.error_timeout)
    is DataError.Unknown -> UiText.Resource(R.string.error_unknown)
}

fun ValidationError.asUiText(): UiText = UiText.Resource(
    when (this) {
        ValidationError.REQUIRED -> R.string.error_required
        ValidationError.INVALID_EMAIL -> R.string.error_invalid_email
        ValidationError.PASSWORD_TOO_SHORT -> R.string.error_password_too_short
        ValidationError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
        ValidationError.TERMS_NOT_ACCEPTED -> R.string.error_terms_not_accepted
        ValidationError.INVALID_OTP -> R.string.error_invalid_otp
    }
)
