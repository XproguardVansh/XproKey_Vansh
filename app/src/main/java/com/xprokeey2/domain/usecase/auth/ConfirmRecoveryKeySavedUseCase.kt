package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.RecoveryKeyRepository
import javax.inject.Inject

/** Called once the user has saved their recovery key; it is then wiped from the device. */
class ConfirmRecoveryKeySavedUseCase @Inject constructor(
    private val recoveryKeyRepository: RecoveryKeyRepository,
) {
    suspend operator fun invoke(email: String) {
        recoveryKeyRepository.deletePendingKey(email.trim())
    }
}
