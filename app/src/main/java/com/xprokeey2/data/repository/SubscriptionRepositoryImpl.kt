package com.xprokeey2.data.repository

import com.xprokeey2.data.local.payment.PaymentStateStorage
import com.xprokeey2.data.mapper.toDomain
import com.xprokeey2.data.remote.datasource.AccountRemoteDataSource
import com.xprokeey2.data.remote.datasource.PaymentRemoteDataSource
import com.xprokeey2.domain.model.AccountSummary
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.domain.util.map
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val accountRemote: AccountRemoteDataSource,
    private val paymentRemote: PaymentRemoteDataSource,
    private val paymentState: PaymentStateStorage,
) : SubscriptionRepository {

    override suspend fun getAccountSummary(): Resource<AccountSummary> =
        accountRemote.getDashboardSummary().map { it.toDomain() }

    override suspend fun getBilling(): Resource<Billing> = accountRemote.getBilling().map { it.toDomain() }

    override suspend fun isTrialEligible(): Resource<Boolean> = paymentRemote.getTrialStatus().map { it.eligible }

    override suspend fun startTrial(plan: SubscriptionPlan): Resource<Unit> =
        paymentRemote.startTrial(plan.value).map { }

    override suspend fun createSubscription(plan: SubscriptionPlan): Resource<String> =
        when (val result = paymentRemote.createSubscription(plan.value)) {
            is Resource.Success -> result.data.subscriptionId?.takeIf { it.isNotBlank() }
                ?.let { Resource.Success(it) }
                ?: Resource.Error(DataError.Unknown("No subscription_id in the response"))
            is Resource.Error -> result
        }

    override suspend fun cancelSubscription(): Resource<CancelledSubscription> =
        paymentRemote.cancelSubscription().map { it.toDomain() }

    override suspend fun isPaymentPending(): Boolean = paymentState.isPaymentPending()

    override suspend fun setPaymentPending(pending: Boolean) = paymentState.setPaymentPending(pending)
}
