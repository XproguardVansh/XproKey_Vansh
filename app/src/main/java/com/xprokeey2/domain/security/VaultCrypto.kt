package com.xprokeey2.domain.security

import com.xprokeey2.domain.model.NewVault

/** Client-side zero-knowledge crypto (PBKDF2-SHA256 + AES-256-GCM), compatible with the web app. */
interface VaultCrypto {

    /** Generates salt, vault key and recovery key, and encrypts the vault key both ways. */
    suspend fun createVault(masterPassword: String): NewVault

    /** Returns the Base64 vault key, or null if the master password / data doesn't decrypt it. */
    suspend fun unlockVaultKey(
        masterPassword: String,
        masterSalt: String,
        encryptedVaultKey: String,
    ): String?

    /**
     * Returns the Base64 vault key, or null if [recoveryKey] doesn't decrypt it. Like the password,
     * the recovery key goes through PBKDF2 with the account's [masterSalt].
     */
    suspend fun recoverVaultKey(
        recoveryKey: String,
        masterSalt: String,
        encryptedVaultKeyRecovery: String,
    ): String?

    /**
     * Returns [vaultKey] encrypted with [masterPassword] under the account's existing [masterSalt]
     * (password reset: the web app keeps the salt and only replaces encrypted_vault_key).
     */
    suspend fun lockVaultKey(vaultKey: String, masterPassword: String, masterSalt: String): String
}
