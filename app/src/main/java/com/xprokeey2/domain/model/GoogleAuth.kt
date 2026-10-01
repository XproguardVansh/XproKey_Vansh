package com.xprokeey2.domain.model

/**
 * A session POST /auth/google (or /auth/setup-vault-password) sent back. Like the web, it is saved on
 * the device only once the vault is open, so until then it lives in memory.
 */
data class GoogleSession(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
    /** Where the server wants the user to go next, e.g. "dashboard". */
    val nextAction: String?,
    val masterSalt: String,
    val encryptedVaultKey: String,
) {
    val hasVaultKeys: Boolean
        get() = masterSalt.isNotBlank() && encryptedVaultKey.isNotBlank()

    /**
     * Same rule as a password login ([LoginResult.needsAccountSetup]), except that no next_action
     * means the dashboard, as in the web's Google button (`next_action || "dashboard"`).
     */
    val needsAccountSetup: Boolean
        get() = nextAction != null && nextAction != LoginResult.NEXT_ACTION_DASHBOARD

    // Keeps the tokens out of logs.
    override fun toString(): String = "GoogleSession(email=${user.email}, nextAction=$nextAction)"
}

/** What POST /auth/google answered, by the web's cases. */
sealed interface GoogleAuthOutcome {

    /** First Google sign-in (next_action "vault_setup_required"): the account needs a master password. */
    data class VaultSetupRequired(val setupToken: String, val name: String, val email: String) : GoogleAuthOutcome {
        override fun toString(): String = "VaultSetupRequired(email=$email)"
    }

    /** A known account: signed in. [message] is the server's. */
    data class SignedIn(val session: GoogleSession, val message: String?) : GoogleAuthOutcome
}

/** What happens after Google's account picker, like the web's Google button. */
sealed interface GoogleSignInStep {

    /** New Google account: "Create Your Master Password". */
    data class CreateMasterPassword(val setupToken: String, val name: String, val email: String) : GoogleSignInStep {
        override fun toString(): String = "CreateMasterPassword(email=$email)"
    }

    /** The account has a vault: "Enter Master Password" opens it, and only then is [session] saved. */
    data class EnterMasterPassword(val session: GoogleSession) : GoogleSignInStep

    /** No vault on the account (the web says this shouldn't normally happen): signed in already. */
    data class SignedIn(val needsAccountSetup: Boolean, val message: String?) : GoogleSignInStep
}

/** A master password was created for a new Google account. */
data class GoogleVaultSetup(
    /** Shown once in the recovery-key dialog; never sent to the server. */
    val recoveryKey: String,
    /** False when the server sent no session back: the user then signs in with Google again. */
    val isSignedIn: Boolean,
    val needsAccountSetup: Boolean,
) {
    override fun toString(): String = "GoogleVaultSetup(isSignedIn=$isSignedIn, needsAccountSetup=$needsAccountSetup)"
}

/**
 * The strength shown while a Google user creates the master password (the web's getPasswordStrength):
 * a point each for 8+ characters, 12+ characters, an uppercase letter, a digit and a symbol.
 */
enum class MasterPasswordStrength {
    WEAK,
    MEDIUM,
    STRONG;

    companion object {
        fun of(password: String): MasterPasswordStrength {
            var score = 0
            if (password.length >= 8) score++
            if (password.length >= 12) score++
            if (password.any { it in 'A'..'Z' }) score++
            if (password.any { it in '0'..'9' }) score++
            if (password.any { it !in 'A'..'Z' && it !in 'a'..'z' && it !in '0'..'9' }) score++
            return when {
                score <= 2 -> WEAK
                score <= 4 -> MEDIUM
                else -> STRONG
            }
        }
    }
}
