package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * The server creates the Razorpay subscription and returns its ID for Checkout. From here until billing
 * shows it active, the payment counts as pending so an interrupted payment is checked again later.
 */
class CreateSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(plan: SubscriptionPlan): Resource<String> {
        val result = repository.createSubscription(plan)
        if (result is Resource.Success) repository.setPaymentPending(true)
        return result
    }
}
