package com.xprokeey2.data.remote.dto.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of start-yearly-trial and create-subscription: "quarterly" or "yearly". */
@Serializable
data class PlanRequestDto(val plan: String)

/** GET /payments/trial-status */
@Serializable
data class TrialStatusDto(val eligible: Boolean = false)

/** POST /payments/start-yearly-trial: `{"status": "trial_started", "plan": "yearly", "trial_end": "…"}`. */
@Serializable
data class StartTrialResponseDto(
    val status: String? = null,
    val plan: String? = null,
    @SerialName("trial_end") val trialEnd: String? = null,
)

/** POST /payments/create-subscription: the Razorpay subscription to open Checkout with. */
@Serializable
data class CreateSubscriptionResponseDto(
    val success: Boolean? = null,
    @SerialName("subscription_id") val subscriptionId: String? = null,
    val plan: String? = null,
)

/** POST /payments/cancel-subscription: `{"status": "subscription_cancelled", "message": "…"}`. */
@Serializable
data class CancelSubscriptionResponseDto(
    val status: String? = null,
    val message: String? = null,
)
