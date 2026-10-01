package com.xprokeey2.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** POST /auth/google */
@Serializable
data class GoogleAuthRequestDto(
    @SerialName("id_token") val idToken: String,
) {
    override fun toString(): String = "GoogleAuthRequestDto(idToken=…)"
}

/**
 * Answer of POST /auth/google, and of POST /auth/setup-vault-password (the web reads that one as a
 * login answer). Every field is optional, as in the web's GoogleAuthResponse type: a new account
 * gets next_action "vault_setup_required" + setup_token, a known one the tokens and its vault keys.
 */
@Serializable
data class GoogleAuthResponseDto(
    val message: String? = null,
    @SerialName("next_action") val nextAction: String? = null,
    @SerialName("setup_token") val setupToken: String? = null,
    val user: GoogleUserDto? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("master_salt") val masterSalt: String? = null,
    @SerialName("encrypted_vault_key") val encryptedVaultKey: String? = null,
) {
    override fun toString(): String = "GoogleAuthResponseDto(nextAction=$nextAction, user=$user)"

    companion object {
        const val NEXT_ACTION_VAULT_SETUP = "vault_setup_required"
    }
}

@Serializable
data class GoogleUserDto(
    /** A string like "0507" elsewhere; read either way. */
    @SerialName("user_id") val userId: JsonPrimitive? = null,
    val name: String? = null,
    val email: String? = null,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("subscription_expires_at") val subscriptionExpiresAt: String? = null,
)

/** POST /auth/setup-vault-password (Authorization: Bearer setup_token) */
@Serializable
data class SetupVaultPasswordRequestDto(
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    @SerialName("master_salt") val masterSalt: String,
    @SerialName("encrypted_vault_key") val encryptedVaultKey: String,
    @SerialName("encrypted_vault_key_recovery") val encryptedVaultKeyRecovery: String,
) {
    override fun toString(): String = "SetupVaultPasswordRequestDto(…)"
}
