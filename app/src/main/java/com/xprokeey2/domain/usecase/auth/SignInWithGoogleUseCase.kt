package com.xprokeey2.domain.usecase.auth

import com.xprokeey2.domain.model.GoogleAuthOutcome
import com.xprokeey2.domain.model.GoogleSignInStep
import com.xprokeey2.domain.repository.GoogleAuthRepository
import com.xprokeey2.domain.security.VaultSession
import com.xprokeey2.domain.util.Resource
import javax.inject.Inject

/**
 * Sends Google's ID token to the server and works out the next step, like the web's Google button:
 * a new account creates its master password; an account with a vault opens it with the master
 * password before anything is saved; an account without a vault is signed in straight away.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: GoogleAuthRepository,
    private val vaultSession: VaultSession,
) {
    suspend operator fun invoke(idToken: String): Resource<GoogleSignInStep> {
        val outcome = when (val result = repository.signIn(idToken)) {
            is Resource.Error -> return result
            is Resource.Success -> result.data
        }

        val step = when (outcome) {
            is GoogleAuthOutcome.VaultSetupRequired -> GoogleSignInStep.CreateMasterPassword(
                setupToken = outcome.setupToken,
                name = outcome.name,
                email = outcome.email,
            )
            is GoogleAuthOutcome.SignedIn -> if (outcome.session.hasVaultKeys) {
                GoogleSignInStep.EnterMasterPassword(outcome.session)
            } else {
                // Nothing to unlock: stay signed in with the vault locked, as a password login does.
                repository.saveSession(outcome.session)
                vaultSession.lock()
                GoogleSignInStep.SignedIn(
                    needsAccountSetup = outcome.session.needsAccountSetup,
                    message = outcome.message,
                )
            }
        }
        return Resource.Success(step)
    }
}
