package com.xprokeey2.data.local.session

/** Everything persisted after a successful login. */
data class SessionEntity(
    val accessToken: String,
    val refreshToken: String,
    /** Vault key wrapped by the master password; unwrapped client-side (AES-256-GCM). */
    val encryptedVaultKey: String,
    val masterSalt: String,
    val userId: String,
    val name: String,
    val email: String,
)

data class SessionProfile(
    val userId: String,
    val name: String,
    val email: String,
)
