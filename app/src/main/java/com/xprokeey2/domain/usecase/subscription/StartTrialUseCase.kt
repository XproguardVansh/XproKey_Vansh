package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** The 7-day free trial on the chosen plan: no card and no Razorpay; the server decides eligibility. */
class StartTrialUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(plan: SubscriptionPlan): Resource<Unit> = repository.startTrial(plan)
}
