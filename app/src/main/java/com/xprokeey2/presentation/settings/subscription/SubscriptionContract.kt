package com.xprokeey2.presentation.settings.subscription

import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.SubscriptionOverview
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge

/** Settings > Subscription: a Personal plan or a Business license. */
data class SubscriptionUiState(
    val user: UserBadge? = null,
    val isLoading: Boolean = true,
    val overview: SubscriptionOverview? = null,
)

/** Where the Personal card's button goes, by subscription status. */
enum class SubscriptionNextStep {
    /** Active: "Manage Subscription". */
    MANAGE,

    /** Trial: "Continue with … Plan" to checkout. */
    CONTINUE_WITH_PLAN,

    /** Anything else: "Get Access". */
    GET_ACCESS;

    companion object {
        fun of(status: String?): SubscriptionNextStep = when (status?.lowercase()) {
            Billing.STATUS_ACTIVE -> MANAGE
            Billing.STATUS_TRIAL -> CONTINUE_WITH_PLAN
            else -> GET_ACCESS
        }
    }
}

sealed interface SubscriptionEvent {
    data class ShowMessage(val message: UiText) : SubscriptionEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : SubscriptionEvent
}
