package com.xprokeey2.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.xprokeey2.data.crypto.VaultCryptoImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same checks as the JVM unit tests, but on Android's own crypto provider: its
 * PBKDF2WithHmacSHA256 must produce the web app's keys (incl. UTF-8 for non-ASCII passwords).
 */
@RunWith(AndroidJUnit4::class)
class VaultCryptoDeviceTest {

    private val crypto = VaultCryptoImpl()

    /** Produced in Node's WebCrypto with the web app's deriveKey + encryptText (see VaultCryptoImplTest). */
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
    fun keyDerivationMatchesPbkdf2Reference() {
        assertEquals(
            "5e4d68da9281a8120b484f05cbd50e015dfad2e0589c3b11c52f988322604cbb",
            crypto.deriveKey("pässwörd✓-€", "2RhJvQ8+aYKXzTvGAuC+/Q==").joinToString("") { "%02x".format(it) },
        )
    }

    @Test
    fun opensWebVaultWithPasswordAndRecoveryKey() = runBlocking {
        assertEquals(
            WebVector.VAULT_KEY,
            crypto.unlockVaultKey(WebVector.PASSWORD, WebVector.MASTER_SALT, WebVector.ENCRYPTED_VAULT_KEY),
        )
        assertEquals(
            WebVector.VAULT_KEY,
            crypto.recoverVaultKey(WebVector.RECOVERY_KEY, WebVector.MASTER_SALT, WebVector.ENCRYPTED_VAULT_KEY_RECOVERY),
        )
    }

    @Test
    fun vaultCreatedOnDeviceOpensBothWays() = runBlocking {
        val vault = crypto.createVault("Andr0id#Pass-é")
        val keys = vault.encryptedKeys
        val viaPassword = crypto.unlockVaultKey("Andr0id#Pass-é", keys.masterSalt, keys.encryptedVaultKey)
        val viaRecovery = crypto.recoverVaultKey(vault.recoveryKey, keys.masterSalt, keys.encryptedVaultKeyRecovery)
        assertEquals(viaPassword, viaRecovery)
        assertEquals(44, viaPassword!!.length)
    }
}
