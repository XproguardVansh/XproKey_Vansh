package com.xprokeey2.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of /forgot-password (request body is [EmailRequestDto]). */
@Serializable
data class ForgotPasswordResponseDto(
    val message: String,
    @SerialName("master_salt") val masterSalt: String? = null,
    @SerialName("encrypted_vault_key_recovery") val encryptedVaultKeyRecovery: String? = null,
)

/**
 * Body of /reset-password, exactly what the web app sends (no salt: it stays the same).
 * [encryptedVaultKey] is null (and therefore omitted) for accounts that have no vault.
 */
@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val otp: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    @SerialName("encrypted_vault_key") val encryptedVaultKey: String? = null,
)
