package com.xprokeey2.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val message: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("encrypted_vault_key") val encryptedVaultKey: String? = null,
    @SerialName("master_salt") val masterSalt: String? = null,
    @SerialName("next_action") val nextAction: String? = null,
    val user: LoginUserDto,
)

@Serializable
data class LoginUserDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val email: String,
    @SerialName("account_type") val accountType: String? = null,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
    @SerialName("subscription_expires_at") val subscriptionExpiresAt: String? = null,
)
