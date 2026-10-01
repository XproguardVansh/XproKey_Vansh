package com.xprokeey2.data.mapper

import com.xprokeey2.data.local.session.SessionEntity
import com.xprokeey2.data.remote.dto.auth.GoogleAuthResponseDto
import com.xprokeey2.data.remote.dto.auth.GoogleUserDto
import com.xprokeey2.domain.model.GoogleAuthOutcome
import com.xprokeey2.domain.model.GoogleSession
import com.xprokeey2.domain.model.User
import kotlinx.serialization.json.contentOrNull

/**
 * The web's order: "vault_setup_required" with a setup token means a new account; otherwise an
 * access token means signed in. Null for an answer with neither.
 */
fun GoogleAuthResponseDto.toOutcome(): GoogleAuthOutcome? {
    val token = setupToken
    if (nextAction == GoogleAuthResponseDto.NEXT_ACTION_VAULT_SETUP && !token.isNullOrBlank()) {
        return GoogleAuthOutcome.VaultSetupRequired(
            setupToken = token,
            name = user?.name.orEmpty(),
            email = user?.email.orEmpty(),
        )
    }
    val session = toSessionOrNull() ?: return null
    return GoogleAuthOutcome.SignedIn(session = session, message = message)
}

/** The session in the answer, or null without an access token. Missing vault keys stay blank. */
fun GoogleAuthResponseDto.toSessionOrNull(): GoogleSession? {
    val access = accessToken?.takeIf { it.isNotBlank() } ?: return null
    return GoogleSession(
        accessToken = access,
        refreshToken = refreshToken.orEmpty(),
        user = user.toDomain(),
        nextAction = nextAction,
        masterSalt = masterSalt.orEmpty(),
        encryptedVaultKey = encryptedVaultKey.orEmpty(),
    )
}

fun GoogleUserDto?.toDomain() = User(
    userId = this?.userId?.contentOrNull.orEmpty(),
    name = this?.name.orEmpty(),
    email = this?.email.orEmpty(),
    accountType = this?.accountType,
    subscriptionStatus = this?.subscriptionStatus,
    subscriptionExpiresAt = this?.subscriptionExpiresAt,
)

fun GoogleSession.toSessionEntity() = SessionEntity(
    accessToken = accessToken,
    refreshToken = refreshToken,
    encryptedVaultKey = encryptedVaultKey,
    masterSalt = masterSalt,
    userId = user.userId,
    name = user.name,
    email = user.email,
)
