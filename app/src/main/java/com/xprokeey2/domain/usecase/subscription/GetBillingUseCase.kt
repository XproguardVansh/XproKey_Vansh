package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** GET /me/billing. An active subscription also settles a payment that was still pending. */
class GetBillingUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Resource<Billing> {
        val result = repository.getBilling()
        if (result is Resource.Success && result.data.isActive) repository.setPaymentPending(false)
        return result
    }
}
