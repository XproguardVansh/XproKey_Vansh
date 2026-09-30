package com.xprokeey2.data.mapper

import com.xprokeey2.data.remote.dto.account.BillingDto
import com.xprokeey2.data.remote.dto.account.DashboardSummaryDto
import com.xprokeey2.data.remote.dto.account.SecuritySettingsDto
import com.xprokeey2.data.remote.dto.payment.CancelSubscriptionResponseDto
import com.xprokeey2.domain.model.AccountSummary
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.ServerSessionTimeout
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import java.time.Instant
import java.time.OffsetDateTime

fun SecuritySettingsDto.toDomain() = ServerSessionTimeout(
    duration = TimeoutDuration.of(timeoutDuration),
    action = TimeoutAction.of(timeoutAction),
)

fun SessionTimeoutSettings.toDto() = SecuritySettingsDto(
    timeoutDuration = duration.value,
    timeoutAction = action.value,
)

fun BillingDto.toDomain() = Billing(
    planType = planType?.takeIf { it.isNotBlank() },
    subscriptionStatus = subscriptionStatus,
    isTrial = isTrial ?: false,
    expiresAt = expiresAt.toTimestamp(),
    nextBillingAt = nextBillingAt.toTimestamp(),
    trialDaysLeft = trialDaysLeft,
    isExpiringSoon = isExpiringSoon ?: false,
    autoRenew = autoRenew,
)

fun DashboardSummaryDto.toDomain() = AccountSummary(
    accountType = accountType,
    status = status,
    daysLeft = daysLeft,
    nextBillingAt = nextBillingAt.toTimestamp(),
    licenseStatus = licenseStatus,
    licenseExpiresAt = licenseExpiresAt.toTimestamp(),
)

fun CancelSubscriptionResponseDto.toDomain() = CancelledSubscription(status = status, message = message)

private fun String?.toTimestamp(): Instant? = this?.takeIf { it.isNotBlank() }?.let {
    runCatching { Instant.parse(it) }.getOrNull() ?: runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull()
}
