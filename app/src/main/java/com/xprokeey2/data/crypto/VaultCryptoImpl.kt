package com.xprokeey2.data.crypto

import com.xprokeey2.domain.model.EncryptedVaultKeys
import com.xprokeey2.domain.model.NewVault
import com.xprokeey2.domain.security.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlin.io.encoding.Base64

/**
 * Mirrors the web app's `lib/api/crypto.ts` (tech doc §1):
 * - master salt: 16 random bytes, Base64
 * - vault / recovery key: 32 random bytes, Base64
 * - master key: PBKDF2-SHA256, 100,000 iterations, salt = UTF-8 bytes of the Base64 salt *string*
 * - AES-256-GCM with a fresh 12-byte IV; payload = Base64(iv || ciphertext+tag)
 * - the Base64 vault-key string is what gets encrypted (matches the 72-byte blobs the server returns)
 * - password reset: recovery key opens the vault key, which is re-locked under the *same* salt
 *   (verified against a real reset payload from the web app)
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
                encryptedVaultKey = encrypt(vaultKey, deriveMasterKey(masterPassword, masterSalt)),
                encryptedVaultKeyRecovery = encrypt(vaultKey, Base64.decode(recoveryKey)),
            ),
            recoveryKey = recoveryKey,
        )
    }

    override suspend fun unlockVaultKey(
        masterPassword: String,
        masterSalt: String,
        encryptedVaultKey: String,
    ): String? = withContext(Dispatchers.Default) {
        runCatching {
            decodeVaultKey(decrypt(encryptedVaultKey, deriveMasterKey(masterPassword, masterSalt)))
        }.getOrNull()
    }

    override suspend fun recoverVaultKey(
        recoveryKey: String,
        encryptedVaultKeyRecovery: String,
    ): String? = withContext(Dispatchers.Default) {
        runCatching {
            decodeVaultKey(decrypt(encryptedVaultKeyRecovery, Base64.decode(recoveryKey)))
        }.getOrNull()
    }

    override suspend fun lockVaultKey(vaultKey: String, masterPassword: String, masterSalt: String): String =
        withContext(Dispatchers.Default) { encrypt(vaultKey, deriveMasterKey(masterPassword, masterSalt)) }

    /** Expected: the Base64 key string. Also accept the raw 32 key bytes. */
    private fun decodeVaultKey(plain: ByteArray): String =
        if (plain.size == KEY_SIZE) Base64.encode(plain) else String(plain, Charsets.UTF_8)

    internal fun deriveMasterKey(masterPassword: String, masterSalt: String): ByteArray =
        Pbkdf2.deriveKey(
            password = masterPassword.toByteArray(Charsets.UTF_8),
            salt = masterSalt.toByteArray(Charsets.UTF_8),
            iterations = PBKDF2_ITERATIONS,
            keyLength = KEY_SIZE,
        )

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
        const val PBKDF2_ITERATIONS = 100_000
        const val SALT_SIZE = 16
        const val KEY_SIZE = 32
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
