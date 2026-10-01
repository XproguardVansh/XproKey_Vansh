package com.xprokeey2.domain.model

/** Key material the server stores. It only ever sees ciphertext (zero-knowledge). */
data class EncryptedVaultKeys(
    /** Base64 of 16 random bytes. */
    val masterSalt: String,
    /** Vault key encrypted with the key derived from the master password. */
    val encryptedVaultKey: String,
    /** Vault key encrypted with the emergency recovery key. */
    val encryptedVaultKeyRecovery: String,
)

/** Result of creating a brand-new vault on signup. */
data class NewVault(
    val encryptedKeys: EncryptedVaultKeys,
    /** Base64 of the 256-bit recovery key. Shown to the user once; never sent to the server. */
    val recoveryKey: String,
    /** The new vault key itself (Base64), for opening the vault right away (Google sign-up). */
    val vaultKey: String,
)
