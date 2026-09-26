package com.xprokeey2.presentation.onboarding.license

import com.xprokeey2.presentation.util.UiText

data class ActivateLicenseUiState(
    val licenseKey: String = "",
    val licenseKeyError: UiText? = null,
    val isLoading: Boolean = false,
)

sealed interface ActivateLicenseAction {
    data class LicenseKeyChanged(val licenseKey: String) : ActivateLicenseAction
    data object Submit : ActivateLicenseAction
}

sealed interface ActivateLicenseEvent {
    data class Activated(val organization: String?) : ActivateLicenseEvent

    /** Login token missing or rejected: send the user back to sign in. */
    data class SessionExpired(val message: UiText) : ActivateLicenseEvent
}
