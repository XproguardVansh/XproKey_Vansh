package com.xprokeey2.presentation.settings

import com.xprokeey2.R
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.presentation.onboarding.plan.PlanEvent
import com.xprokeey2.presentation.onboarding.plan.checkoutFailure
import com.xprokeey2.presentation.onboarding.plan.isAlreadyActive
import com.xprokeey2.presentation.payment.razorpayErrorDescription
import com.xprokeey2.presentation.session.timeoutDeadline
import com.xprokeey2.presentation.settings.billing.BillingBadge
import com.xprokeey2.presentation.settings.billing.BillingUiState
import com.xprokeey2.presentation.settings.subscription.SubscriptionNextStep
import com.xprokeey2.presentation.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/** The web Subscription, Billing and checkout pages' rules, and the session timeout's clock. */
class SubscriptionPresentationTest {

    private fun billing(status: String, autoRenew: Boolean? = null) = Billing(
        planType = "yearly",
        subscriptionStatus = status,
        isTrial = false,
        expiresAt = null,
        nextBillingAt = null,
        trialDaysLeft = null,
        isExpiringSoon = false,
        autoRenew = autoRenew,
    )

    @Test
    fun datesLookLikeTheWebsEnInFormat() {
        assertEquals("26 Sept 2027", Instant.parse("2027-09-26T10:00:00Z").billingDate(ZoneOffset.UTC))
        assertEquals("1 Jan 2027", Instant.parse("2027-01-01T00:00:00Z").billingDate(ZoneOffset.UTC))
    }

    @Test
    fun capitalisationMatchesTheWeb() {
        assertEquals("Yearly", "yearly".capitalizeFirst())
        assertEquals("Active", "active".capitalizeWords())
        assertEquals("Past Due", "past due".capitalizeWords())
    }

    @Test
    fun theSubscriptionButtonFollowsTheStatus() {
        assertEquals(SubscriptionNextStep.MANAGE, SubscriptionNextStep.of("active"))
        assertEquals(SubscriptionNextStep.CONTINUE_WITH_PLAN, SubscriptionNextStep.of("Trial"))
        assertEquals(SubscriptionNextStep.GET_ACCESS, SubscriptionNextStep.of("inactive"))
        assertEquals(SubscriptionNextStep.GET_ACCESS, SubscriptionNextStep.of(null))
    }

    @Test
    fun billingBadgeInTheWebsOrder() {
        assertEquals(BillingBadge.ACTIVE, BillingUiState(billing = billing("active")).badge)
        assertEquals(BillingBadge.CANCELLED, BillingUiState(billing = billing("active", autoRenew = false)).badge)
        assertEquals(BillingBadge.TRIAL, BillingUiState(billing = billing("trial")).badge)
        assertEquals(BillingBadge.INACTIVE, BillingUiState(billing = billing("expired")).badge)

        val renewing = BillingUiState(billing = billing("active", autoRenew = true))
        assertTrue(renewing.isRenewing)
        assertFalse(renewing.isCancelled)
    }

    @Test
    fun checkoutFailuresFollowTheGuide() {
        fun messageId(event: PlanEvent) = ((event as PlanEvent.ShowMessage).message as UiText.Resource).id

        assertTrue(checkoutFailure(DataError.SessionExpired) is PlanEvent.SignInRequired)
        assertEquals(R.string.error_no_internet, messageId(checkoutFailure(DataError.NoInternet)))
        // Raw payment-provider errors and server config problems aren't shown as sent.
        assertEquals(R.string.checkout_unable_to_continue, messageId(checkoutFailure(DataError.Server(500, "BAD_REQUEST_ERROR: plan_xyz"))))
        assertEquals(R.string.checkout_unable_to_continue, messageId(checkoutFailure(DataError.Server(400, "Invalid plan"))))

        assertTrue(DataError.Server(400, "You already have an active subscription").isAlreadyActive())
        assertFalse(DataError.Server(500, "You already have an active subscription").isAlreadyActive())
        assertFalse(DataError.Server(400, "Invalid plan").isAlreadyActive())
    }

    @Test
    fun razorpayErrorsShowTheirDescription() {
        assertEquals(
            "Your payment didn't go through as it was declined by the bank.",
            razorpayErrorDescription(
                """{"error":{"code":"BAD_REQUEST_ERROR","description":"Your payment didn't go through as it was declined by the bank.","source":"bank"}}"""
            ),
        )
        assertNull(razorpayErrorDescription("""{"error":{"code":"NETWORK_ERROR"}}"""))
        assertNull(razorpayErrorDescription("Payment processing cancelled by user"))
        assertNull(razorpayErrorDescription(null))
    }

    @Test
    fun timeoutDeadlineCountsFromTheLastTouch() {
        assertEquals(1_000L + 5 * 60_000L, timeoutDeadline(1_000L, TimeoutDuration.FIVE_MINUTES))
        assertEquals(240 * 60_000L, timeoutDeadline(0L, TimeoutDuration.FOUR_HOURS))
        assertNull(timeoutDeadline(1_000L, TimeoutDuration.NEVER))
    }
}
