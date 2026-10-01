package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.GoogleAuthOutcome
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.util.Resource

/** Signing in and signing up with Google, like the web's Google button. */
interface GoogleAuthRepository {

    /** POST /auth/google with the ID token from Google's account picker. Nothing is saved yet. */
    suspend fun signIn(idToken: String): Resource<GoogleAuthOutcome>

    /**
     * POST /auth/setup-vault-password, authorised by the setup token: the new account's master password
     * and its encrypted vault keys. Returns the session the server sends back, or null if it sent none.
     */
    suspend fun setupVaultPassword(
        setupToken: String,
        password: String,
        confirmPassword: String,
        vaultKeys: EncryptedVaultKeys,
    ): Resource<GoogleSession?>

    /** Saves the session on the device, as a password login does. */
    suspend fun saveSession(session: GoogleSession)
}
