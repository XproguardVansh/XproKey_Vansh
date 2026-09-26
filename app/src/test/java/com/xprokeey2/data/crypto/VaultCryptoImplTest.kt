package com.xprokeey2.data.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.encoding.Base64

class VaultCryptoImplTest {

    private val crypto = VaultCryptoImpl()

    /**
     * Produced in Node's WebCrypto with the web app's `deriveKey` and `encryptText(text, secret, masterSalt)`
     * as described in the encryption architecture doc: both copies of the vault key are wrapped under
     * PBKDF2(secret, master_salt), where secret is the password or the recovery key.
     */
    private object WebVector {
        const val PASSWORD = "Sup3r\$ecret-ñ✓"
        const val MASTER_SALT = "2RhJvQ8+aYKXzTvGAuC+/Q=="
        const val VAULT_KEY = "TQkzi8jV90LIkYuRaWsDS+2eg5H290vBdeLpW2GgCv8="
        const val RECOVERY_KEY = "6MDnmCGlH698SS49Y3Qhf33tnoofwiM9bUBNfg8at8s="
        const val ENCRYPTED_VAULT_KEY =
            "ikEqj27BCHC7sxqh4Eh1lLfYOmoR2JY+5kb2dxq96BtPgrFKONAHc0VB2Pg0m+4yMVc30jj5DjKIoaX6IkY2RfAD270eekhK"
        const val ENCRYPTED_VAULT_KEY_RECOVERY =
            "2NtiA6gyeGU6GzNe5KIP/RJy7rYkcj8L2E2DrofZwva6svxNbJrLlTzFCtugjMjLesnt+4CGWl4+kbpSVRogSuANkaTkDRfx"
    }

    @Test
    fun unlocksVaultKeyCreatedByWebApp() = runBlocking {
        val vaultKey = crypto.unlockVaultKey(
            masterPassword = WebVector.PASSWORD,
            masterSalt = WebVector.MASTER_SALT,
            encryptedVaultKey = WebVector.ENCRYPTED_VAULT_KEY,
        )
        assertEquals(WebVector.VAULT_KEY, vaultKey)
    }

    @Test
    fun recoveryKeyFromWebAppRecoversVaultKey() = runBlocking {
        assertEquals(
            WebVector.VAULT_KEY,
            crypto.recoverVaultKey(
                recoveryKey = WebVector.RECOVERY_KEY,
                masterSalt = WebVector.MASTER_SALT,
                encryptedVaultKeyRecovery = WebVector.ENCRYPTED_VAULT_KEY_RECOVERY,
            ),
        )
    }

    /** Regression: the recovery key is a PBKDF2 secret, never a raw AES key (that broke Android <-> web). */
    @Test
    fun recoveryCopyIsNotLockedWithTheRawRecoveryKey() {
        val rawKeyAttempt = runCatching {
            crypto.decrypt(WebVector.ENCRYPTED_VAULT_KEY_RECOVERY, Base64.decode(WebVector.RECOVERY_KEY))
        }
        assertTrue(rawKeyAttempt.isFailure)
    }

    @Test
    fun wrongRecoveryKeyOrSaltRecoversNothing() = runBlocking {
        val otherKey = Base64.encode(ByteArray(32) { 7 })
        assertNull(crypto.recoverVaultKey(otherKey, WebVector.MASTER_SALT, WebVector.ENCRYPTED_VAULT_KEY_RECOVERY))
        assertNull(crypto.recoverVaultKey("not base64 at all", WebVector.MASTER_SALT, WebVector.ENCRYPTED_VAULT_KEY_RECOVERY))
        assertNull(
            crypto.recoverVaultKey(WebVector.RECOVERY_KEY, "AAAAAAAAAAAAAAAAAAAAAA==", WebVector.ENCRYPTED_VAULT_KEY_RECOVERY)
        )
    }

    @Test
    fun wrongMasterPasswordReturnsNull() = runBlocking {
        assertNull(
            crypto.unlockVaultKey(
                masterPassword = "not-the-password",
                masterSalt = WebVector.MASTER_SALT,
                encryptedVaultKey = WebVector.ENCRYPTED_VAULT_KEY,
            )
        )
    }

    /** Doc pitfall #7: a decrypted value that isn't a 44-char / 32-byte Base64 key is rejected. */
    @Test
    fun decryptedValueThatIsNotAVaultKeyIsRejected() = runBlocking {
        val salt = "2RhJvQ8+aYKXzTvGAuC+/Q=="
        val notAVaultKey = crypto.lockVaultKey("hello world", "pw-12345", salt)
        assertNull(crypto.unlockVaultKey("pw-12345", salt, notAVaultKey))
        assertNull(crypto.recoverVaultKey("pw-12345", salt, notAVaultKey))
    }

    @Test
    fun createdVaultOpensWithPasswordAndWithRecoveryKey() = runBlocking {
        val password = "Correct-Horse-9"
        val vault = crypto.createVault(password)
        val keys = vault.encryptedKeys

        assertEquals(16, Base64.decode(keys.masterSalt).size)
        assertEquals(32, Base64.decode(vault.recoveryKey).size)
        // Same size as the blobs the real server returns: 12 IV + 44 (Base64 key) + 16 tag.
        assertEquals(72, Base64.decode(keys.encryptedVaultKey).size)
        assertEquals(72, Base64.decode(keys.encryptedVaultKeyRecovery).size)

        val viaPassword = crypto.unlockVaultKey(password, keys.masterSalt, keys.encryptedVaultKey)
        val viaRecovery = crypto.recoverVaultKey(vault.recoveryKey, keys.masterSalt, keys.encryptedVaultKeyRecovery)
        assertEquals(viaPassword, viaRecovery)
        assertEquals(32, Base64.decode(viaPassword!!).size)
    }

    /** What the web app does on reset: same vault key, new password, SAME salt; only encrypted_vault_key changes. */
    @Test
    fun passwordResetKeepsVaultKeyAndSalt() = runBlocking {
        val vault = crypto.createVault("Old-Password1")
        val keys = vault.encryptedKeys

        val vaultKey = crypto.recoverVaultKey(vault.recoveryKey, keys.masterSalt, keys.encryptedVaultKeyRecovery)!!
        val newEncryptedVaultKey = crypto.lockVaultKey(vaultKey, "New-Password2", keys.masterSalt)

        assertEquals(vaultKey, crypto.unlockVaultKey("New-Password2", keys.masterSalt, newEncryptedVaultKey))
        assertNull(crypto.unlockVaultKey("Old-Password1", keys.masterSalt, newEncryptedVaultKey))
        assertEquals(72, Base64.decode(newEncryptedVaultKey).size)
        // The recovery key keeps working after the reset.
        assertEquals(vaultKey, crypto.recoverVaultKey(vault.recoveryKey, keys.masterSalt, keys.encryptedVaultKeyRecovery))
    }

    /** Expected values from Python's hashlib.pbkdf2_hmac("sha256", ..., 100_000, 32). */
    @Test
    fun keyDerivationMatchesPbkdf2Reference() {
        assertEquals(
            "0394a2ede332c9a13eb82e9b24631604c31df978b4e2f0fbd2c549944f9d79a5",
            crypto.deriveKey("password", "salt").toHex(),
        )
        // Non-ASCII password: the provider must turn chars into UTF-8 bytes, like the web's TextEncoder.
        assertEquals(
            "5e4d68da9281a8120b484f05cbd50e015dfad2e0589c3b11c52f988322604cbb",
            crypto.deriveKey("pässwörd✓-€", "2RhJvQ8+aYKXzTvGAuC+/Q==").toHex(),
        )
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    @Test
    fun everyEncryptionUsesAFreshIv() {
        val key = ByteArray(32) { it.toByte() }
        assertNotEquals(crypto.encrypt("same text", key), crypto.encrypt("same text", key))
    }

    /**
     * Produced in Node's WebCrypto with the tech doc's `encryptWithVaultKey(text, vaultKey)`: the raw
     * vault key is the AES-GCM key (no PBKDF2), as the web app does for card numbers and CVCs.
     */
    private object CardVector {
        const val NUMBER = "4111111111111111"
        const val ENCRYPTED_NUMBER = "tqh+LtR1Igu2hYHOqJuEhGjWg7FsBgSPesDkFnLkxlSn3c6yoKeU3g/1BPE="
        const val CVC = "123"
        const val ENCRYPTED_CVC = "tX0frXm3hOznXmr+UdOW8yuUS8F2PjLKpkyZVNVhnQ=="
    }

    @Test
    fun decryptsCardSecretsEncryptedByWebApp() = runBlocking {
        assertEquals(CardVector.NUMBER, crypto.decryptWithVaultKey(CardVector.ENCRYPTED_NUMBER, WebVector.VAULT_KEY))
        assertEquals(CardVector.CVC, crypto.decryptWithVaultKey(CardVector.ENCRYPTED_CVC, WebVector.VAULT_KEY))
    }

    @Test
    fun cardSecretsRoundTripUnderVaultKey() = runBlocking {
        val encrypted = crypto.encryptWithVaultKey(CardVector.NUMBER, WebVector.VAULT_KEY)
        assertNotEquals(CardVector.NUMBER, encrypted)
        assertEquals(CardVector.NUMBER, crypto.decryptWithVaultKey(encrypted, WebVector.VAULT_KEY))
    }

    @Test
    fun cardSecretsDontOpenWithAnotherVaultKeyOrAsPlainText() = runBlocking {
        val otherVaultKey = WebVector.RECOVERY_KEY // any other 32-byte key
        assertNull(crypto.decryptWithVaultKey(CardVector.ENCRYPTED_NUMBER, otherVaultKey))
        // Cards saved from Postman store the number unencrypted.
        assertNull(crypto.decryptWithVaultKey("4111111111111111", WebVector.VAULT_KEY))
    }
}
