package com.xprokeey2.presentation.profile

import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.ProfileOverview
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Profile (the web's /profile): the account, then its plan or its business license. */
data class ProfileUiState(
    val user: UserBadge? = null,
    val isLoading: Boolean = true,
    val overview: ProfileOverview? = null,
)

/** The coloured line on the plan or license card, in the web's order of checks. */
enum class ProfileBanner {
    BUSINESS,
    CANCELLED,
    EXPIRING_SOON,
    ACTIVE,
    TRIAL,
    NO_SUBSCRIPTION;

    companion object {
        fun of(overview: ProfileOverview): ProfileBanner {
            if (overview.profile.isBusiness) return BUSINESS
            val billing = overview.billing
            val isActive = billing?.isActive == true
            val isCancelled = isActive && billing?.autoRenew == false
            val isRenewing = isActive && billing?.autoRenew != false
            return when {
                isCancelled -> CANCELLED
                isRenewing && billing?.isExpiringSoon == true -> EXPIRING_SOON
                isRenewing -> ACTIVE
                billing?.subscriptionStatus == Billing.STATUS_TRIAL -> TRIAL
                else -> NO_SUBSCRIPTION
            }
        }
    }
}

sealed interface ProfileEvent {
    data class ShowMessage(val message: UiText) : ProfileEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : ProfileEvent
}
