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
    DataError.VaultLocked -> UiText.Resource(R.string.error_vault_locked)
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
        ValidationError.CARD_LABEL_REQUIRED -> R.string.error_card_label_required
        ValidationError.CARD_NUMBER_REQUIRED -> R.string.error_card_number_required
        ValidationError.INVALID_CARD_NUMBER -> R.string.error_invalid_card_number
        ValidationError.EXPIRY_MONTH_INVALID -> R.string.error_expiry_month_invalid
        ValidationError.EXPIRY_YEAR_REQUIRED -> R.string.error_expiry_year_required
        ValidationError.EXPIRY_YEAR_TWO_DIGITS -> R.string.error_expiry_year_two_digits
        ValidationError.CARD_EXPIRED -> R.string.error_card_expired
        ValidationError.CVC_REQUIRED -> R.string.error_cvc_required
        ValidationError.CVC_MUST_BE_3_DIGITS -> R.string.error_cvc_3_digits
        ValidationError.CVC_MUST_BE_4_DIGITS -> R.string.error_cvc_4_digits
    }
)
