package com.xprokeey2.data.mapper

import com.xprokeey2.data.local.session.SessionEntity
import com.xprokeey2.data.remote.dto.auth.ForgotPasswordResponseDto
import com.xprokeey2.data.remote.dto.auth.LoginResponseDto
import com.xprokeey2.data.remote.dto.auth.LoginUserDto
import com.xprokeey2.data.remote.dto.auth.SignupResponseDto
import com.xprokeey2.data.remote.dto.auth.SignupUserDto
import com.xprokeey2.domain.model.ForgotPasswordResult
import com.xprokeey2.domain.model.LoginResult
import com.xprokeey2.domain.model.SignupResult
import com.xprokeey2.domain.model.User

fun SignupUserDto.toDomain() = User(
    userId = userId,
    name = name,
    email = email,
)

fun LoginUserDto.toDomain() = User(
    userId = userId,
    name = name,
    email = email,
    accountType = accountType,
    subscriptionStatus = subscriptionStatus,
    subscriptionExpiresAt = subscriptionExpiresAt,
)

fun SignupResponseDto.toDomain() = SignupResult(
    message = message,
    user = user.toDomain(),
)

fun LoginResponseDto.toDomain() = LoginResult(
    message = message,
    user = user.toDomain(),
    nextAction = nextAction,
    masterSalt = masterSalt.orEmpty(),
    encryptedVaultKey = encryptedVaultKey.orEmpty(),
)

fun LoginResponseDto.toSessionEntity() = SessionEntity(
    accessToken = accessToken,
    refreshToken = refreshToken,
    encryptedVaultKey = encryptedVaultKey.orEmpty(),
    masterSalt = masterSalt.orEmpty(),
    userId = user.userId,
    name = user.name,
    email = user.email,
)

fun ForgotPasswordResponseDto.toDomain() = ForgotPasswordResult(
    message = message,
    masterSalt = masterSalt.orEmpty(),
    encryptedVaultKeyRecovery = encryptedVaultKeyRecovery.orEmpty(),
)
