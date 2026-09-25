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
