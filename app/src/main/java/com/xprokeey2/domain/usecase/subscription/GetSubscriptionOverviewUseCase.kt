package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.SubscriptionOverview
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Settings > Subscription, like the web page: the account summary, plus the plan name from billing for
 * Personal accounts (a failed billing call just leaves the name out).
 */
class GetSubscriptionOverviewUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Resource<SubscriptionOverview> {
        val summary = when (val result = repository.getAccountSummary()) {
            is Resource.Success -> result.data
            is Resource.Error -> return result
        }
        if (!summary.isPersonal) {
            return Resource.Success(
                SubscriptionOverview.Business(
                    licenseStatus = summary.licenseStatus,
                    licenseExpiresAt = summary.licenseExpiresAt,
                )
            )
        }
        val planType = (repository.getBilling() as? Resource.Success)?.data?.planType
        return Resource.Success(
            SubscriptionOverview.Personal(
                status = summary.status,
                daysLeft = summary.daysLeft,
                nextBillingAt = summary.nextBillingAt,
                planType = planType,
            )
        )
    }
}
