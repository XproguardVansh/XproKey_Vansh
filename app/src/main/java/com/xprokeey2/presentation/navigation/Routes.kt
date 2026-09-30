package com.xprokeey2.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * [email] pre-fills the form (e.g. after verifying an account or resetting the password);
 * [message] is shown once when the screen opens.
 */
@Serializable
data class LoginRoute(val email: String? = null, val message: String? = null)

@Serializable
data object SignupRoute

@Serializable
data class VerifyEmailRoute(val email: String)

@Serializable
data object TermsRoute

@Serializable
data object PrivacyPolicyRoute

@Serializable
data class ForgotPasswordRoute(val email: String = "")

/**
 * [masterSalt] and [encryptedVaultKeyRecovery] come from /forgot-password (the recovery blob is
 * only usable with the user's recovery key). Passing them here keeps them across process death,
 * e.g. while the user switches to their email app to read the OTP.
 */
@Serializable
data class ResetPasswordRoute(
    val email: String,
    val masterSalt: String,
    val encryptedVaultKeyRecovery: String,
)

/** Personal / Business chooser, shown after login until the account is set up. */
@Serializable
data object AccountTypeRoute

/**
 * Personal: "Choose your plan". [subscriptionEnded] when the server said the subscription or trial is
 * over: then this is the only screen left, and its Back button logs out (the user's rule).
 */
@Serializable
data class PlanRoute(val subscriptionEnded: Boolean = false)

@Serializable
data object ActivateLicenseRoute

/** Home of the signed-in app, shared by Personal and Business accounts. */
@Serializable
data object DashboardRoute

@Serializable
data object CardsRoute

/** Add card, or Edit card when [cardId] is set. */
@Serializable
data class CardFormRoute(val cardId: Long? = null)

@Serializable
data class CardDetailsRoute(val cardId: Long)

/** Passwords list; [showWeakItems] opens it on the "Weak Items" filter (dashboard card). */
@Serializable
data class PasswordsRoute(val showWeakItems: Boolean = false)

/** Add password, or Edit ("Update Vault Details") when [itemId] is set. */
@Serializable
data class PasswordFormRoute(val itemId: Long? = null)

@Serializable
data class PasswordDetailsRoute(val itemId: Long)

/** Tools > Generator */
@Serializable
data object GeneratorRoute

/** Tools > Export */
@Serializable
data object ExportRoute

/** Tools > Import */
@Serializable
data object ImportRoute

/** Settings > Change password */
@Serializable
data object ChangePasswordRoute

/** Settings > Security: the session timeout. */
@Serializable
data object SecurityRoute

/** Settings > Subscription: the Personal plan or the Business license. */
@Serializable
data object SubscriptionRoute

/** Manage Subscription (the web's /billing): billing details and cancelling renewal. */
@Serializable
data object BillingRoute

/** Profile, from the account menu: the account and its plan or business license. */
@Serializable
data object ProfileRoute

/** After a session timeout with "Lock": the master password opens the vault again. */
@Serializable
data object LockRoute

/** Settings > About > App Info */
@Serializable
data object AppInfoRoute

/** Settings > About > FAQ */
@Serializable
data object FaqRoute

/** Support: "Help & Support", the user's tickets. */
@Serializable
data object SupportRoute

/** "Submit a Support Ticket" */
@Serializable
data object NewTicketRoute

@Serializable
data class SupportTicketRoute(val ticketId: String)
