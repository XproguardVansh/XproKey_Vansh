package com.xprokeey2.presentation.settings.billing

import com.xprokeey2.domain.model.Billing
import com.xprokeey2.presentation.util.UiText

/** The status badge of Manage Subscription, in the web's order of checks. */
enum class BillingBadge { CANCELLED, ACTIVE, TRIAL, INACTIVE }

/** Manage Subscription (the web's /billing page). */
data class BillingUiState(
    val isLoading: Boolean = true,
    val billing: Billing? = null,
    val isCancelDialogVisible: Boolean = false,
    val isCancelling: Boolean = false,
) {
    private val isActive: Boolean get() = billing?.isActive == true

    /** Renewal switched off: the web reads `auto_renew`, which the server doesn't send yet. */
    val isCancelled: Boolean get() = isActive && billing?.autoRenew == false
    val isRenewing: Boolean get() = isActive && billing?.autoRenew != false
    val isTrial: Boolean get() = billing?.subscriptionStatus == Billing.STATUS_TRIAL

    val badge: BillingBadge
        get() = when {
            isCancelled -> BillingBadge.CANCELLED
            isRenewing -> BillingBadge.ACTIVE
            isTrial -> BillingBadge.TRIAL
            else -> BillingBadge.INACTIVE
        }
}

sealed interface BillingAction {
    data object CancelSubscriptionClicked : BillingAction
    data object DismissCancelDialog : BillingAction
    data object ConfirmCancel : BillingAction
}

sealed interface BillingEvent {
    data class ShowMessage(val message: UiText) : BillingEvent

    /** Billing couldn't be read: back to the previous screen, showing [message]. */
    data class LoadFailed(val message: UiText) : BillingEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : BillingEvent
}
