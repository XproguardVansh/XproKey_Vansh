package com.xprokeey2.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequestDto(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("confirm_password") val confirmPassword: String,
    @SerialName("master_salt") val masterSalt: String,
    @SerialName("encrypted_vault_key") val encryptedVaultKey: String,
    @SerialName("encrypted_vault_key_recovery") val encryptedVaultKeyRecovery: String,
)

@Serializable
data class SignupResponseDto(
    val message: String,
    val user: SignupUserDto,
)

@Serializable
data class SignupUserDto(
    @SerialName("user_id") val userId: String,
    val name: String,
    val email: String,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)
