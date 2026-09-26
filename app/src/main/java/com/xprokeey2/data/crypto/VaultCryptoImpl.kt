package com.xprokeey2.data.crypto

import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.NewVault
import com.xprokeey2.domain.security.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Byte-for-byte port of the web app's crypto ("XproKey - End-to-End Encryption Architecture" §3-4):
 * - master salt: 16 random bytes, Base64; vault key and recovery key: 32 random bytes, Base64
 * - Key A = PBKDF2-SHA256(secret, salt), 100,000 iterations, 32 bytes, where secret is the password
 *   *or the recovery key* (its Base64 text) and salt is the UTF-8 bytes of the master_salt *string*
 * - AES-256-GCM, fresh 12-byte IV, no AAD; payload = Base64(iv || ciphertext+tag)
 * - the vault key is stored as two copies of its Base64 text: under Key A[password]
 *   (encrypted_vault_key) and under Key A[recoveryKey] (encrypted_vault_key_recovery)
 * - password reset re-wraps the same vault key under Key A[new password] with the same salt
 */
class VaultCryptoImpl @Inject constructor() : VaultCrypto {

    private val random = SecureRandom()

    override suspend fun createVault(masterPassword: String): NewVault = withContext(Dispatchers.Default) {
        val masterSalt = randomBase64(SALT_SIZE)
        val vaultKey = randomBase64(KEY_SIZE)
        val recoveryKey = randomBase64(KEY_SIZE)

        NewVault(
            encryptedKeys = EncryptedVaultKeys(
                masterSalt = masterSalt,
                encryptedVaultKey = wrap(vaultKey, secret = masterPassword, masterSalt = masterSalt),
                encryptedVaultKeyRecovery = wrap(vaultKey, secret = recoveryKey, masterSalt = masterSalt),
            ),
            recoveryKey = recoveryKey,
        )
    }

    override suspend fun unlockVaultKey(
        masterPassword: String,
        masterSalt: String,
        encryptedVaultKey: String,
    ): String? = withContext(Dispatchers.Default) {
        unwrap(encryptedVaultKey, secret = masterPassword, masterSalt = masterSalt)
    }

    override suspend fun recoverVaultKey(
        recoveryKey: String,
        masterSalt: String,
        encryptedVaultKeyRecovery: String,
    ): String? = withContext(Dispatchers.Default) {
        unwrap(encryptedVaultKeyRecovery, secret = recoveryKey, masterSalt = masterSalt)
    }

    override suspend fun lockVaultKey(vaultKey: String, masterPassword: String, masterSalt: String): String =
        withContext(Dispatchers.Default) { wrap(vaultKey, secret = masterPassword, masterSalt = masterSalt) }

    /** Doc §4.4 / §6.5: the raw 32-byte vault key is the AES key, no PBKDF2. */
    override suspend fun encryptWithVaultKey(plainText: String, vaultKey: String): String =
        withContext(Dispatchers.Default) { encrypt(plainText, vaultKeyBytes(vaultKey)) }

    override suspend fun decryptWithVaultKey(payload: String, vaultKey: String): String? =
        withContext(Dispatchers.Default) {
            runCatching { String(decrypt(payload, vaultKeyBytes(vaultKey)), Charsets.UTF_8) }.getOrNull()
        }

    private fun vaultKeyBytes(vaultKey: String): ByteArray =
        Base64.decode(vaultKey).also { require(it.size == KEY_SIZE) { "Vault key must be $KEY_SIZE bytes" } }

    /** Web `encryptText(vaultKey, secret, masterSalt)`: AES-GCM under Key A[secret]. */
    private fun wrap(vaultKey: String, secret: String, masterSalt: String): String =
        encrypt(vaultKey, deriveKey(secret, masterSalt))

    /**
     * Web `decryptText(payload, secret, masterSalt)`. Null when the secret is wrong (GCM tag check) or
     * the result isn't a vault key: 44 Base64 chars decoding to 32 bytes (doc pitfall #7).
     */
    private fun unwrap(payload: String, secret: String, masterSalt: String): String? =
        runCatching { String(decrypt(payload, deriveKey(secret, masterSalt)), Charsets.UTF_8) }
            .getOrNull()
            ?.takeIf(::isVaultKey)

    private fun isVaultKey(value: String): Boolean =
        value.length == VAULT_KEY_BASE64_LENGTH &&
            runCatching { Base64.decode(value).size == KEY_SIZE }.getOrDefault(false)

    /**
     * Key A (doc §6.2): PBKDF2WithHmacSHA256 over [secret] and the UTF-8 bytes of the Base64 salt
     * *text* (not decoded). The provider turns the password chars into UTF-8, like WebCrypto's TextEncoder.
     */
    internal fun deriveKey(secret: String, masterSalt: String): ByteArray {
        val spec = PBEKeySpec(
            secret.toCharArray(),
            masterSalt.toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_SIZE * 8,
        )
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    internal fun encrypt(plainText: String, key: ByteArray): String {
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
        return Base64.encode(iv + cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)))
    }

    internal fun decrypt(payload: String, key: ByteArray): ByteArray {
        val bytes = Base64.decode(payload)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_SIZE_BITS, bytes, 0, IV_SIZE),
            )
        }
        return cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)
    }

    private fun randomBase64(size: Int): String = Base64.encode(ByteArray(size).also(random::nextBytes))

    private companion object {
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_ITERATIONS = 100_000
        const val SALT_SIZE = 16
        const val KEY_SIZE = 32
        const val VAULT_KEY_BASE64_LENGTH = 44
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
