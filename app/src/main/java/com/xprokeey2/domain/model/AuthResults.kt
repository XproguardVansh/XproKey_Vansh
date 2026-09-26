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

    /**
     * True until the user finished account setup (e.g. activated a business license): the server
     * sends next_action "dashboard" once setup is done, and something else (e.g. "payment") before.
     */
    val needsAccountSetup: Boolean
        get() = nextAction != NEXT_ACTION_DASHBOARD

    companion object {
        const val NEXT_ACTION_DASHBOARD = "dashboard"
    }
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
