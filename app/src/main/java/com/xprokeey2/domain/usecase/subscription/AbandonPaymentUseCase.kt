package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.repository.SubscriptionRepository
import javax.inject.Inject

/** Checkout was closed or the payment failed: nothing is pending any more. */
class AbandonPaymentUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke() = repository.setPaymentPending(false)
}
