package com.xprokeey2.presentation.onboarding.plan

import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.presentation.payment.PaymentResult
import com.xprokeey2.presentation.util.UiText

/** "Choose your plan": the free trial when the server allows it, otherwise payment with Razorpay. */
data class PlanUiState(
    /** "Checking subscription status…" until billing and trial eligibility are known. */
    val isCheckingAccess: Boolean = true,
    val trialEligible: Boolean? = null,
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.YEARLY,
    /** From the tap until the trial started or the payment sheet is closed, like the web button. */
    val isLoading: Boolean = false,
)

sealed interface PlanAction {
    data class PlanSelected(val plan: SubscriptionPlan) : PlanAction
    data object Continue : PlanAction
    data class PaymentFinished(val result: PaymentResult) : PlanAction
}

sealed interface PlanEvent {
    data class ShowMessage(val message: UiText) : PlanEvent

    /** Open Razorpay Checkout for the subscription the server created. */
    data class OpenCheckout(val subscriptionId: String, val description: UiText) : PlanEvent

    /** Trial started or payment made: on to the Dashboard. */
    data object Finished : PlanEvent

    /** Nothing to buy: open Manage Subscription and show [message]. */
    data class AlreadyActive(val message: UiText) : PlanEvent

    /** Session over: back to Login. */
    data class SignInRequired(val message: UiText) : PlanEvent
}
