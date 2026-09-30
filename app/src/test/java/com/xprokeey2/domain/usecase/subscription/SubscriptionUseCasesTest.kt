package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.AccountSummary
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.CheckoutAccess
import com.xprokeey2.domain.model.SubscriptionOverview
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SubscriptionUseCasesTest {

    private class FakeRepository(
        var summary: Resource<AccountSummary> = Resource.Error(DataError.NoInternet),
        /** Answers to successive billing calls; the last one repeats. */
        var billing: List<Resource<Billing>> = listOf(Resource.Error(DataError.NoInternet)),
        var trialEligible: Resource<Boolean> = Resource.Success(false),
        var created: Resource<String> = Resource.Success("sub_123"),
        var pending: Boolean = false,
    ) : SubscriptionRepository {
        var billingCalls = 0

        override suspend fun getAccountSummary() = summary
        override suspend fun getBilling(): Resource<Billing> = billing[minOf(billingCalls++, billing.lastIndex)]
        override suspend fun isTrialEligible() = trialEligible
        override suspend fun startTrial(plan: SubscriptionPlan): Resource<Unit> = Resource.Success(Unit)
        override suspend fun createSubscription(plan: SubscriptionPlan) = created
        override suspend fun cancelSubscription(): Resource<CancelledSubscription> =
            Resource.Success(CancelledSubscription(CancelledSubscription.STATUS_CANCELLED, null))
        override suspend fun isPaymentPending() = pending
        override suspend fun setPaymentPending(pending: Boolean) {
            this.pending = pending
        }
    }

    private fun billing(status: String, planType: String? = "yearly") = Billing(
        planType = planType,
        subscriptionStatus = status,
        isTrial = status == Billing.STATUS_TRIAL,
        expiresAt = null,
        nextBillingAt = null,
        trialDaysLeft = null,
        isExpiringSoon = false,
        autoRenew = null,
    )

    private fun summary(accountType: String) = AccountSummary(
        accountType = accountType,
        status = "trial",
        daysLeft = 3,
        nextBillingAt = null,
        licenseStatus = "active",
        licenseExpiresAt = Instant.parse("2027-09-26T00:00:00Z"),
    )

    @Test
    fun personalOverviewTakesThePlanNameFromBilling() = runBlocking {
        val repository = FakeRepository(
            summary = Resource.Success(summary("personal")),
            billing = listOf(Resource.Success(billing("trial", planType = "yearly"))),
        )

        val overview = (GetSubscriptionOverviewUseCase(repository)() as Resource.Success).data

        assertEquals(SubscriptionOverview.Personal(status = "trial", daysLeft = 3, nextBillingAt = null, planType = "yearly"), overview)
    }

    @Test
    fun aFailedBillingCallOnlyLeavesThePlanNameOut() = runBlocking {
        val repository = FakeRepository(summary = Resource.Success(summary("personal")))

        val overview = (GetSubscriptionOverviewUseCase(repository)() as Resource.Success).data

        assertNull((overview as SubscriptionOverview.Personal).planType)
    }

    @Test
    fun businessOverviewShowsTheLicenseWithoutAskingBilling() = runBlocking {
        val repository = FakeRepository(summary = Resource.Success(summary("business")))

        val overview = (GetSubscriptionOverviewUseCase(repository)() as Resource.Success).data

        assertEquals(SubscriptionOverview.Business("active", Instant.parse("2027-09-26T00:00:00Z")), overview)
        assertEquals(0, repository.billingCalls)
    }

    @Test
    fun checkoutSendsActiveSubscribersAwayAndSettlesAPendingPayment() = runBlocking {
        val repository = FakeRepository(billing = listOf(Resource.Success(billing("active"))), pending = true)

        assertEquals(CheckoutAccess.AlreadyActive, GetCheckoutAccessUseCase(GetBillingUseCase(repository), repository)())
        assertFalse(repository.pending)
    }

    @Test
    fun checkoutOffersTheTrialOnlyWhenTheServerSaysSo() = runBlocking {
        val eligible = FakeRepository(billing = listOf(Resource.Success(billing("inactive"))), trialEligible = Resource.Success(true))
        val unknown = FakeRepository(trialEligible = Resource.Error(DataError.Server(500, "Failed to fetch user")))

        assertEquals(CheckoutAccess.Available(trialEligible = true), GetCheckoutAccessUseCase(GetBillingUseCase(eligible), eligible)())
        // A failed billing check doesn't stop checkout; a failed trial check means no trial (web).
        assertEquals(CheckoutAccess.Available(trialEligible = false), GetCheckoutAccessUseCase(GetBillingUseCase(unknown), unknown)())
    }

    @Test
    fun aCreatedSubscriptionIsPendingUntilBillingShowsItActive() = runBlocking {
        val repository = FakeRepository()

        val result = CreateSubscriptionUseCase(repository)(SubscriptionPlan.YEARLY)

        assertEquals("sub_123", (result as Resource.Success).data)
        assertTrue(repository.pending)
    }

    @Test
    fun aFailedCreationLeavesNothingPending() = runBlocking {
        val repository = FakeRepository(created = Resource.Error(DataError.Server(500, "Plan ID not configured")))

        CreateSubscriptionUseCase(repository)(SubscriptionPlan.QUARTERLY)

        assertFalse(repository.pending)
    }

    @Test
    fun waitingChecksBillingUntilTheWebhookActivatedIt() = runBlocking {
        val repository = FakeRepository(
            billing = listOf(Resource.Error(DataError.Server(403, "")), Resource.Success(billing("inactive")), Resource.Success(billing("active"))),
            pending = true,
        )

        assertTrue(WaitForActiveSubscriptionUseCase(repository)(intervalMillis = 0))
        assertEquals(3, repository.billingCalls)
        assertFalse(repository.pending)
    }

    @Test
    fun waitingGivesUpAfterTenChecks() = runBlocking {
        val repository = FakeRepository(billing = listOf(Resource.Success(billing("inactive"))), pending = true)

        assertFalse(WaitForActiveSubscriptionUseCase(repository)(intervalMillis = 0))
        assertEquals(10, repository.billingCalls)
        assertTrue(repository.pending)
    }

    @Test
    fun aPaywallIsConfirmedWithTwoBillingChecks() = runBlocking {
        val lagging = FakeRepository(billing = listOf(Resource.Success(billing("inactive")), Resource.Success(billing("active"))))
        val unpaid = FakeRepository(billing = listOf(Resource.Success(billing("inactive"))))

        assertTrue(ConfirmSubscriptionActiveUseCase(lagging)(intervalMillis = 0))
        assertFalse(ConfirmSubscriptionActiveUseCase(unpaid)(intervalMillis = 0))
        assertEquals(2, unpaid.billingCalls)
    }

    @Test
    fun theForegroundCheckOnlyRunsForAPendingPayment() = runBlocking {
        val idle = FakeRepository(billing = listOf(Resource.Success(billing("active"))))
        val pending = FakeRepository(billing = listOf(Resource.Success(billing("active"))), pending = true)

        assertFalse(RecheckPendingPaymentUseCase(idle, ConfirmSubscriptionActiveUseCase(idle))())
        assertEquals(0, idle.billingCalls)
        assertTrue(RecheckPendingPaymentUseCase(pending, ConfirmSubscriptionActiveUseCase(pending))())
        assertFalse(pending.pending)
    }

    @Test
    fun abandoningAPaymentClearsIt() = runBlocking {
        val repository = FakeRepository(pending = true)

        AbandonPaymentUseCase(repository)()

        assertFalse(repository.pending)
    }
}
