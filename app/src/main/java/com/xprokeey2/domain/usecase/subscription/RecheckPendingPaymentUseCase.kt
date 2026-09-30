package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * When the app comes back to the foreground (Razorpay guide 6): if a payment was still pending, billing is
 * read again, which clears it once the webhook has activated the subscription. True when that happened.
 */
class RecheckPendingPaymentUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
    private val confirmActive: ConfirmSubscriptionActiveUseCase,
) {
    suspend operator fun invoke(): Boolean {
        if (!repository.isPaymentPending()) return false
        return confirmActive(attempts = 1)
    }
}
