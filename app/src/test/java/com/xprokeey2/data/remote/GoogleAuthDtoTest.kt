package com.xprokeey2.data.remote

import com.xprokeey2.data.mapper.toOutcome
import com.xprokeey2.data.mapper.toSessionEntity
import com.xprokeey2.data.mapper.toSessionOrNull
import com.xprokeey2.data.remote.dto.auth.GoogleAuthRequestDto
import com.xprokeey2.data.remote.dto.auth.GoogleAuthResponseDto
import com.xprokeey2.data.remote.dto.auth.SetupVaultPasswordRequestDto
import com.xprokeey2.domain.model.GoogleAuthOutcome
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The shapes of the web's GoogleAuthResponse type (lib/api/auth.ts) and its setup call. */
class GoogleAuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun parse(body: String) = json.decodeFromString<GoogleAuthResponseDto>(body)

    @Test
    fun requestBodiesUseTheWebFieldNames() {
        assertEquals("""{"id_token":"abc"}""", json.encodeToString(GoogleAuthRequestDto("abc")))
        assertEquals(
            """{"password":"p","confirm_password":"p","master_salt":"s","encrypted_vault_key":"k","encrypted_vault_key_recovery":"r"}""",
            json.encodeToString(SetupVaultPasswordRequestDto("p", "p", "s", "k", "r")),
        )
    }

    @Test
    fun newGoogleAccountNeedsVaultSetup() {
        val outcome = parse(
            """{"message":"Vault setup required","next_action":"vault_setup_required","setup_token":"setup-123",
               "user":{"name":"Vansh Goel","email":"goelv2610@gmail.com"}}"""
        ).toOutcome()

        assertEquals(GoogleAuthOutcome.VaultSetupRequired("setup-123", "Vansh Goel", "goelv2610@gmail.com"), outcome)
        // The setup token never shows up in logs.
        assertFalse(outcome.toString().contains("setup-123"))
    }

    @Test
    fun returningGoogleUserIsSignedInWithVaultKeys() {
        val outcome = parse(
            """{"message":"Login successful","next_action":"dashboard","access_token":"acc","refresh_token":"ref",
               "master_salt":"salt==","encrypted_vault_key":"key==",
               "user":{"user_id":"0507","name":"Vansh","email":"goelv2610@gmail.com","account_type":"personal",
               "subscription_status":"active","subscription_expires_at":null}}"""
        ).toOutcome() as GoogleAuthOutcome.SignedIn

        val session = outcome.session
        assertEquals("Login successful", outcome.message)
        assertEquals("acc", session.accessToken)
        assertEquals("ref", session.refreshToken)
        assertEquals("0507", session.user.userId)
        assertEquals("personal", session.user.accountType)
        assertTrue(session.hasVaultKeys)
        assertFalse(session.needsAccountSetup)
        assertFalse(session.toString().contains("acc"))

        val entity = session.toSessionEntity()
        assertEquals("salt==", entity.masterSalt)
        assertEquals("key==", entity.encryptedVaultKey)
        assertEquals("goelv2610@gmail.com", entity.email)
    }

    @Test
    fun numericUserIdAndMissingFieldsAreRead() {
        val session = parse(
            """{"next_action":"payment","access_token":"acc","user":{"user_id":507}}"""
        ).toSessionOrNull()!!

        assertEquals("507", session.user.userId)
        assertEquals("", session.refreshToken)
        assertFalse(session.hasVaultKeys)
        assertTrue(session.needsAccountSetup)

        // Like the web's `next_action || "dashboard"`.
        assertFalse(parse("""{"access_token":"acc"}""").toSessionOrNull()!!.needsAccountSetup)
    }

    @Test
    fun answerWithoutTokensOrSetupTokenIsUnusable() {
        assertNull(parse("""{"message":"ok"}""").toOutcome())
        // "vault_setup_required" without its setup token isn't usable either.
        assertNull(parse("""{"next_action":"vault_setup_required"}""").toOutcome())
        assertNull(parse("""{"access_token":""}""").toSessionOrNull())
    }
}
