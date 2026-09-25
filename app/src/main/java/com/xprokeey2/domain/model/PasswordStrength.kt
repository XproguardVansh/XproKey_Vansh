package com.xprokeey2.domain.model

/**
 * Password score from the tech doc (lib/vault-security.ts): one point each for
 * length >= 5, a digit, a special character and an uppercase letter (0..4).
 */
data class PasswordStrength(val score: Int) {

    val level: Level
        get() = when {
            score <= 1 -> Level.WEAK
            score == 2 -> Level.OKAY
            score == 3 -> Level.GOOD
            else -> Level.STRONG
        }

    enum class Level { WEAK, OKAY, GOOD, STRONG }

    companion object {
        const val MAX_SCORE = 4
    }
}
