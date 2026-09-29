package com.xprokeey2.domain.usecase.vault

import java.security.SecureRandom
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.min

private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
private const val NUMBERS = "0123456789"
private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

/** Port of the web generator (Tools > Generator): character sets and "require all types". */
data class PasswordGeneratorOptions(
    val length: Int = 16,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val numbers: Boolean = true,
    val symbols: Boolean = true,
    val avoidAmbiguous: Boolean = false,
    val requireAllTypes: Boolean = true,
) {
    val hasAnyCharset: Boolean get() = uppercase || lowercase || numbers || symbols

    /** Web `charsetSize`: 26 (24 without ambiguous) per letter case, 10 (8) digits, 26 symbols. */
    val charsetSize: Int
        get() {
            var size = 0
            if (uppercase) size += if (avoidAmbiguous) 24 else 26
            if (lowercase) size += if (avoidAmbiguous) 24 else 26
            if (numbers) size += if (avoidAmbiguous) 8 else 10
            if (symbols) size += SYMBOLS.length
            return size
        }
}

/** The Generator page's rating, by entropy bits; [percentage] fills the strength bar. */
enum class GeneratedPasswordStrength(val percentage: Int) {
    WEAK(20),
    FAIR(40),
    GOOD(60),
    STRONG(80),
    EXCELLENT(100);

    companion object {
        /** Web `calculateEntropy`, capped at 256: floor(length × log2(charset size)); 0 for no password. */
        fun entropyBits(password: String, charsetSize: Int): Int {
            if (password.isEmpty() || charsetSize <= 0) return 0
            return min(256, floor(password.length * log2(charsetSize.toDouble())).toInt())
        }

        /** Web `getStrengthInfo`. */
        fun of(entropyBits: Int): GeneratedPasswordStrength = when {
            entropyBits < 28 -> WEAK
            entropyBits < 36 -> FAIR
            entropyBits < 60 -> GOOD
            entropyBits < 80 -> STRONG
            else -> EXCELLENT
        }
    }
}

class GeneratePasswordUseCase @Inject constructor() {

    private val random = SecureRandom()

    /**
     * "" when no character set is selected. The Add/Edit password form uses the defaults (16), the
     * Generator page starts at 20 like the web.
     */
    operator fun invoke(options: PasswordGeneratorOptions = PasswordGeneratorOptions()): String {
        val sets = buildList {
            if (options.uppercase) add(if (options.avoidAmbiguous) UPPERCASE.filterNot { it in "IO" } else UPPERCASE)
            if (options.lowercase) add(if (options.avoidAmbiguous) LOWERCASE.filterNot { it in "lo" } else LOWERCASE)
            if (options.numbers) add(if (options.avoidAmbiguous) NUMBERS.filterNot { it in "01" } else NUMBERS)
            if (options.symbols) add(SYMBOLS)
        }.filter { it.isNotEmpty() }
        val all = sets.joinToString("")
        if (all.isEmpty()) return ""

        val length = maxOf(options.length, if (options.requireAllTypes) sets.size else 0)
        val chars = mutableListOf<Char>()
        if (options.requireAllTypes) sets.forEach { chars += it[random.nextInt(it.length)] }
        while (chars.size < length) chars += all[random.nextInt(all.length)]
        // Fisher-Yates, like the web's shuffleInPlace.
        for (i in chars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            chars[i] = chars[j].also { chars[j] = chars[i] }
        }
        return chars.joinToString("")
    }
}
