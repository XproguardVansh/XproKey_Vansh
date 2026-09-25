package com.xprokeey2.domain.model

data class SignupResult(
    val message: String,
    val user: User,
)

data class LoginResult(
    val message: String,
    val user: User,
    /** Where the server wants the user to go next, e.g. "dashboard". */
    val nextAction: String?,
    val masterSalt: String,
    val encryptedVaultKey: String,
) {
    /** False for accounts created before client-side vault keys existed (server returns ""). */
    val hasVaultKeys: Boolean
        get() = masterSalt.isNotBlank() && encryptedVaultKey.isNotBlank()
}

data class VerifyAccountResult(
    val message: String,
    /** Recovery key saved on this device at signup, if any; shown once in the dialog. */
    val recoveryKey: String?,
)

data class ForgotPasswordResult(
    val message: String,
    /** Current salt; the new password re-locks the vault key under this same salt. */
    val masterSalt: String,
    /** Vault key encrypted with the recovery key; empty for accounts that have no vault. */
    val encryptedVaultKeyRecovery: String,
)
