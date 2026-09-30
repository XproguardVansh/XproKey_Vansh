package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Session timeout "Log out" and the Lock screen's "Log out instead": tokens, saved keys and the vault
 * key are cleared on this device, and so is a pending payment check (it belonged to this account).
 */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend operator fun invoke() {
        authRepository.clearSession()
        subscriptionRepository.setPaymentPending(false)
    }
}
