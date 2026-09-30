package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * After Razorpay says "paid": the server's webhook activates the subscription, so billing is checked
 * like the web does, once a second up to [attempts] times, ignoring errors. True once it's active.
 */
class WaitForActiveSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(attempts: Int = ATTEMPTS, intervalMillis: Long = INTERVAL_MILLIS): Boolean {
        repeat(attempts) {
            val billing = repository.getBilling()
            if (billing is Resource.Success && billing.data.isActive) {
                repository.setPaymentPending(false)
                return true
            }
            delay(intervalMillis)
        }
        return false
    }

    companion object {
        const val ATTEMPTS = 10
        const val INTERVAL_MILLIS = 1_000L
    }
}
