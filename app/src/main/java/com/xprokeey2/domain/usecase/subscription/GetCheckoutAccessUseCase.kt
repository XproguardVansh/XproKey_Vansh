package com.xprokeey2.domain.usecase.subscription

import com.xprokeey2.domain.model.CheckoutAccess
import com.xprokeey2.domain.repository.SubscriptionRepository
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Opening "Choose your plan", like the web checkout: an active subscription has nothing to buy.
 * Otherwise the server says whether the free trial is still available; if that check fails, it isn't.
 * A failed billing check doesn't stop checkout.
 */
class GetCheckoutAccessUseCase @Inject constructor(
    private val getBilling: GetBillingUseCase,
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): CheckoutAccess {
        val billing = getBilling()
        if (billing is Resource.Success && billing.data.isActive) return CheckoutAccess.AlreadyActive

        val eligible = (repository.isTrialEligible() as? Resource.Success)?.data ?: false
        return CheckoutAccess.Available(trialEligible = eligible)
    }
}
