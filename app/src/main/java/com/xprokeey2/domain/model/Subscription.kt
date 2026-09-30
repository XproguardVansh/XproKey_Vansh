package com.xprokeey2.domain.model

import java.time.Instant

/** Personal plans; the server knows which Razorpay plan each one is. */
enum class SubscriptionPlan(val value: String) {
    QUARTERLY("quarterly"),
    YEARLY("yearly"),
}

/** GET /me/billing: the source of truth for what a Personal user may access. */
data class Billing(
    /** e.g. "quarterly" or "yearly"; null before anything was bought. */
    val planType: String?,
    /** "active", "trial", "inactive", … */
    val subscriptionStatus: String?,
    val isTrial: Boolean,
    val expiresAt: Instant?,
    val nextBillingAt: Instant?,
    val trialDaysLeft: Int?,
    val isExpiringSoon: Boolean,
    /** False once renewal was cancelled; the server doesn't send it yet. */
    val autoRenew: Boolean?,
) {
    val isActive: Boolean get() = subscriptionStatus == STATUS_ACTIVE

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_TRIAL = "trial"
    }
}

/** GET /dashboard/summary, the fields Settings > Subscription shows. */
data class AccountSummary(
    val accountType: String?,
    /** Personal: "active", "trial", "inactive", … */
    val status: String?,
    val daysLeft: Int?,
    val nextBillingAt: Instant?,
    /** Business: the organisation's license. */
    val licenseStatus: String?,
    val licenseExpiresAt: Instant?,
) {
    val isPersonal: Boolean get() = accountType == ACCOUNT_PERSONAL

    companion object {
        const val ACCOUNT_PERSONAL = "personal"
    }
}

/** Settings > Subscription: a Personal subscription or a Business license, like the web page. */
sealed interface SubscriptionOverview {
    data class Personal(
        val status: String?,
        val daysLeft: Int?,
        val nextBillingAt: Instant?,
        /** From /me/billing; null when that call failed. */
        val planType: String?,
    ) : SubscriptionOverview

    data class Business(
        val licenseStatus: String?,
        val licenseExpiresAt: Instant?,
    ) : SubscriptionOverview
}

/** What "Choose your plan" can offer: nothing (already paid), a free trial, or payment. */
sealed interface CheckoutAccess {
    data object AlreadyActive : CheckoutAccess
    data class Available(val trialEligible: Boolean) : CheckoutAccess
}

/** POST /payments/cancel-subscription */
data class CancelledSubscription(val status: String?, val message: String?) {
    val isCancelled: Boolean get() = status == STATUS_CANCELLED

    companion object {
        const val STATUS_CANCELLED = "subscription_cancelled"
    }
}
