package com.xprokeey2.domain.usecase.vault

import java.security.SecureRandom
import javax.inject.Inject

/** Port of the web generator (Tools > Generator): character sets and "require all types". */
data class PasswordGeneratorOptions(
    val length: Int = 16,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val numbers: Boolean = true,
    val symbols: Boolean = true,
    val avoidAmbiguous: Boolean = false,
    val requireAllTypes: Boolean = true,
)

class GeneratePasswordUseCase @Inject constructor() {

    private val random = SecureRandom()

    /** "" when no character set is selected. The Add/Edit password form uses the defaults (16). */
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

    private companion object {
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val NUMBERS = "0123456789"
        const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"
    }
}
