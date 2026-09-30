package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.CancelledSubscription
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/** Turns auto-renew off; the user keeps access until the paid period ends. */
class CancelSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Resource<CancelledSubscription> = repository.cancelSubscription()
}
