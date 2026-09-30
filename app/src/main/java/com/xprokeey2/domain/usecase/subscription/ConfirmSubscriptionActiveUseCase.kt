package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Before sending a user to the paywall after a 403 "payment" (Razorpay guide 7.3): a renewal or a new
 * payment can take a moment to reach the server, so billing is read again, twice at most.
 */
class ConfirmSubscriptionActiveUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(attempts: Int = ATTEMPTS, intervalMillis: Long = INTERVAL_MILLIS): Boolean {
        repeat(attempts) { attempt ->
            if (attempt > 0) delay(intervalMillis)
            val billing = repository.getBilling()
            if (billing is Resource.Success && billing.data.isActive) {
                repository.setPaymentPending(false)
                return true
            }
        }
        return false
    }

    companion object {
        const val ATTEMPTS = 2
        const val INTERVAL_MILLIS = 1_000L
    }
}
