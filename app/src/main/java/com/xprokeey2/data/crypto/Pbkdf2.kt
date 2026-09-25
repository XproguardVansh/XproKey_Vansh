package com.xprokeey2.data.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PBKDF2-HMAC-SHA256 (RFC 8018) over raw bytes.
 *
 * Implemented on top of HmacSHA256 because `PBKDF2WithHmacSHA256` only exists from API 26
 * and providers differ in how they turn a char[] password into bytes; WebCrypto uses the
 * exact UTF-8 bytes, so we do too.
 */
internal object Pbkdf2 {

    private const val ALGORITHM = "HmacSHA256"
    private const val HASH_LENGTH = 32

    fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        require(iterations > 0) { "iterations must be > 0" }
        val mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(password, ALGORITHM)) }
        val output = ByteArray(keyLength)
        val blockCount = (keyLength + HASH_LENGTH - 1) / HASH_LENGTH

        for (blockIndex in 1..blockCount) {
            mac.update(salt)
            mac.update(blockIndex.toBigEndianBytes())
            var u = mac.doFinal()
            val block = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (i in block.indices) block[i] = (block[i].toInt() xor u[i].toInt()).toByte()
            }
            val offset = (blockIndex - 1) * HASH_LENGTH
            block.copyInto(output, offset, 0, minOf(HASH_LENGTH, keyLength - offset))
        }
        return output
    }

    private fun Int.toBigEndianBytes() = byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte(),
    )
}
