package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.AccountSummary
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.util.Resource

/** Personal subscriptions (Razorpay through the server) and the account summary. */
interface SubscriptionRepository {

    /** GET /dashboard/summary */
    suspend fun getAccountSummary(): Resource<AccountSummary>

    /** GET /me/billing */
    suspend fun getBilling(): Resource<Billing>

    /** GET /payments/trial-status */
    suspend fun isTrialEligible(): Resource<Boolean>

    /** POST /payments/start-yearly-trial: starts the free trial on [plan], no payment. */
    suspend fun startTrial(plan: SubscriptionPlan): Resource<Unit>

    /** POST /payments/create-subscription: returns the Razorpay subscription ID to pay. */
    suspend fun createSubscription(plan: SubscriptionPlan): Resource<String>

    /** POST /payments/cancel-subscription: renewal stops at the end of the paid period. */
    suspend fun cancelSubscription(): Resource<CancelledSubscription>

    /** True from opening Razorpay Checkout until billing shows the subscription active (or it was abandoned). */
    suspend fun isPaymentPending(): Boolean

    suspend fun setPaymentPending(pending: Boolean)
}
