package com.xprokeey2.data.remote.dto.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET and PUT /me/security: minutes before the session times out ("1"…"240", or "never") and what happens then. */
@Serializable
data class SecuritySettingsDto(
    @SerialName("timeout_duration") val timeoutDuration: String? = null,
    @SerialName("timeout_action") val timeoutAction: String? = null,
)

/** GET /me/billing, the source of truth for what a Personal user may access. */
@Serializable
data class BillingDto(
    @SerialName("plan_type") val planType: String? = null,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("is_trial") val isTrial: Boolean? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("next_billing_at") val nextBillingAt: String? = null,
    @SerialName("trial_days_left") val trialDaysLeft: Int? = null,
    @SerialName("is_expiring_soon") val isExpiringSoon: Boolean? = null,
    /** Read by the web Billing page; the server doesn't send it yet. */
    @SerialName("auto_renew") val autoRenew: Boolean? = null,
)

/** GET /dashboard/summary: the fields the web Subscription page reads. */
@Serializable
data class DashboardSummaryDto(
    @SerialName("account_type") val accountType: String? = null,
    val status: String? = null,
    @SerialName("days_left") val daysLeft: Int? = null,
    @SerialName("next_billing_at") val nextBillingAt: String? = null,
    @SerialName("license_status") val licenseStatus: String? = null,
    @SerialName("license_expires_at") val licenseExpiresAt: String? = null,
)
