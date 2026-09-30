package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.repository.AccountRepository
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * The account menu's "Log out", like the web: POST /logout, then this device forgets the session
 * whatever the server answered. The server call gets a few seconds at most, so a slow network
 * doesn't hold the user back.
 */
class LogOutUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val signOut: SignOutUseCase,
) {
    suspend operator fun invoke(serverTimeoutMillis: Long = SERVER_TIMEOUT_MILLIS) {
        withTimeoutOrNull(serverTimeoutMillis) { accountRepository.logOut() }
        signOut()
    }

    companion object {
        const val SERVER_TIMEOUT_MILLIS = 5_000L
    }
}
