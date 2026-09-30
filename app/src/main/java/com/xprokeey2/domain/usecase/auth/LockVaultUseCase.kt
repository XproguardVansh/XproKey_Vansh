package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.security.VaultSession
import javax.inject.Inject

/** Session timeout "Lock": the vault key leaves memory, the login session stays for the Lock screen. */
class LockVaultUseCase @Inject constructor(
    private val vaultSession: VaultSession,
) {
    operator fun invoke() = vaultSession.lock()
}
