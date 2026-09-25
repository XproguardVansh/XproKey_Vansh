package com.xprokeey2.data.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.io.encoding.Base64

class VaultCryptoImplTest {

    private val crypto = VaultCryptoImpl()

    /**
     * Produced in Node's WebCrypto with `deriveKey` / `encryptWithVaultKey` copied verbatim
     * from the tech doc, so it proves web-made vaults open on Android.
     */
    private object WebVector {
        const val PASSWORD = "Sup3r\$ecret-ñ✓"
        const val MASTER_SALT = "35yLnWFVxjmU+iVyd7eB9A=="
        const val VAULT_KEY = "B8L6OyX8Tl6tTAwW7nkl5QJEm9AEzuJGrJkg/tCWNO8="
        const val RECOVERY_KEY = "/c5mld3IDjCSQItDghPj0+U0ofHL9SFDU2ylU+WSemU="
        const val ENCRYPTED_VAULT_KEY =
            "r4uta3dCQsy/zU4mKL1ZmGgIJAod0fUf3/od3/X7PpPL8wqj28rEjGtJEhWgFh5Y4Hr273e3/3rodWiMqOL+R5TFRuNOp2S8"
        const val ENCRYPTED_VAULT_KEY_RECOVERY =
            "ukduZrnCMzPYJvk3RRCvLR/WLF/NdS2B7C9TpkR3ZYVd6ETqIR5Nn/bj7pKOUyjiRzKWeDt8igqrfeqYjkj/drNCsDWMmVRv"
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
            crypto.recoverVaultKey(WebVector.RECOVERY_KEY, WebVector.ENCRYPTED_VAULT_KEY_RECOVERY),
        )
    }

    @Test
    fun wrongRecoveryKeyRecoversNothing() = runBlocking {
        val otherKey = Base64.encode(ByteArray(32) { 7 })
        assertNull(crypto.recoverVaultKey(otherKey, WebVector.ENCRYPTED_VAULT_KEY_RECOVERY))
        assertNull(crypto.recoverVaultKey("not base64 at all", WebVector.ENCRYPTED_VAULT_KEY_RECOVERY))
    }

    /** What the web app does on reset: same vault key, new password, SAME salt; only encrypted_vault_key changes. */
    @Test
    fun passwordResetKeepsVaultKeyAndSalt() = runBlocking {
        val vault = crypto.createVault("Old-Password1")
        val salt = vault.encryptedKeys.masterSalt

        val vaultKey = crypto.recoverVaultKey(vault.recoveryKey, vault.encryptedKeys.encryptedVaultKeyRecovery)!!
        val newEncryptedVaultKey = crypto.lockVaultKey(vaultKey, "New-Password2", salt)

        assertEquals(vaultKey, crypto.unlockVaultKey("New-Password2", salt, newEncryptedVaultKey))
        assertNull(crypto.unlockVaultKey("Old-Password1", salt, newEncryptedVaultKey))
        assertEquals(72, Base64.decode(newEncryptedVaultKey).size)
        // The recovery key keeps working after the reset.
        assertEquals(vaultKey, crypto.recoverVaultKey(vault.recoveryKey, vault.encryptedKeys.encryptedVaultKeyRecovery))
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

    @Test
    fun createdVaultRoundTripsWithPasswordAndRecoveryKey() = runBlocking {
        val password = "Correct-Horse-9"
        val vault = crypto.createVault(password)
        val keys = vault.encryptedKeys

        assertEquals(16, Base64.decode(keys.masterSalt).size)
        assertEquals(32, Base64.decode(vault.recoveryKey).size)
        // Same size as the blobs the real server returns: 12 IV + 44 (Base64 key) + 16 tag.
        assertEquals(72, Base64.decode(keys.encryptedVaultKey).size)
        assertEquals(72, Base64.decode(keys.encryptedVaultKeyRecovery).size)

        val viaPassword = crypto.unlockVaultKey(password, keys.masterSalt, keys.encryptedVaultKey)
        val viaRecovery = String(
            crypto.decrypt(keys.encryptedVaultKeyRecovery, Base64.decode(vault.recoveryKey)),
            Charsets.UTF_8,
        )
        assertEquals(viaPassword, viaRecovery)
        assertEquals(32, Base64.decode(viaPassword!!).size)
    }

    @Test
    fun everyEncryptionUsesAFreshIv() {
        val key = ByteArray(32) { it.toByte() }
        assertNotEquals(crypto.encrypt("same text", key), crypto.encrypt("same text", key))
    }
}
