package com.xprokeey2.domain.usecase.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The web Generator page's `charsetSize`, `calculateEntropy` and `getStrengthInfo`. */
class GeneratedPasswordStrengthTest {

    @Test
    fun matchesTheWebScreenshot() {
        // Length 20 with all four types: the web shows "129 bits" and "EXCELLENT".
        val options = PasswordGeneratorOptions(length = 20)
        val password = GeneratePasswordUseCase()(options)
        assertEquals(20, password.length)
        assertEquals(88, options.charsetSize)

        val bits = GeneratedPasswordStrength.entropyBits(password, options.charsetSize)
        assertEquals(129, bits)
        assertEquals(GeneratedPasswordStrength.EXCELLENT, GeneratedPasswordStrength.of(bits))
    }

    @Test
    fun charsetSize() {
        assertEquals(82, PasswordGeneratorOptions(avoidAmbiguous = true).charsetSize) // 24 + 24 + 8 + 26
        assertEquals(10, PasswordGeneratorOptions(uppercase = false, lowercase = false, symbols = false).charsetSize)
        val nothing = PasswordGeneratorOptions(uppercase = false, lowercase = false, numbers = false, symbols = false)
        assertEquals(0, nothing.charsetSize)
        assertFalse(nothing.hasAnyCharset)
        assertTrue(PasswordGeneratorOptions(uppercase = false).hasAnyCharset)
    }

    @Test
    fun entropyIsFlooredAndCapped() {
        assertEquals(0, GeneratedPasswordStrength.entropyBits("", 88))
        assertEquals(0, GeneratedPasswordStrength.entropyBits("abcd", 0))
        // 4 digits: 4 × log2(10) = 13.28 → 13
        assertEquals(13, GeneratedPasswordStrength.entropyBits("1234", 10))
        // 40 of 88: 258.4 → capped at 256
        assertEquals(256, GeneratedPasswordStrength.entropyBits("x".repeat(40), 88))
    }

    @Test
    fun ratingThresholds() {
        assertEquals(GeneratedPasswordStrength.WEAK, GeneratedPasswordStrength.of(27))
        assertEquals(GeneratedPasswordStrength.FAIR, GeneratedPasswordStrength.of(28))
        assertEquals(GeneratedPasswordStrength.FAIR, GeneratedPasswordStrength.of(35))
        assertEquals(GeneratedPasswordStrength.GOOD, GeneratedPasswordStrength.of(36))
        assertEquals(GeneratedPasswordStrength.GOOD, GeneratedPasswordStrength.of(59))
        assertEquals(GeneratedPasswordStrength.STRONG, GeneratedPasswordStrength.of(60))
        assertEquals(GeneratedPasswordStrength.STRONG, GeneratedPasswordStrength.of(79))
        assertEquals(GeneratedPasswordStrength.EXCELLENT, GeneratedPasswordStrength.of(80))
        assertEquals(40, GeneratedPasswordStrength.FAIR.percentage)
    }
}
