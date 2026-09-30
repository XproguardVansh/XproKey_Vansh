package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.SessionRepository
import com.xprokeey2.domain.security.VaultSession
import javax.inject.Inject

/**
 * A screen asked the user to sign in again. True when the saved session is fine and only the vault
 * key is missing from memory (e.g. the app was reopened): the master password on the Lock screen is
 * then enough. Otherwise the user has to log in.
 */
class NeedsUnlockUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(): Boolean =
        vaultSession.vaultKey == null &&
            sessionRepository.hasSession() &&
            !sessionRepository.getEncryptedVaultKey().isNullOrBlank() &&
            !sessionRepository.getMasterSalt().isNullOrBlank()
}
