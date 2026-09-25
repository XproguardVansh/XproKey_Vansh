package com.xprokeey2.domain.usecase.validation

import com.xprokeey2.domain.model.PasswordStrength
import javax.inject.Inject

/** Same scoring as the web app's `passwordScore` (tech doc §2). */
class EvaluatePasswordStrengthUseCase @Inject constructor() {

    operator fun invoke(password: String): PasswordStrength {
        var score = 0
        if (password.length >= 5) score++
        if (DIGIT.containsMatchIn(password)) score++
        if (SPECIAL.containsMatchIn(password)) score++
        if (UPPERCASE.containsMatchIn(password)) score++
        return PasswordStrength(score)
    }

    private companion object {
        val DIGIT = Regex("[0-9]")
        val SPECIAL = Regex("[^A-Za-z0-9]")
        val UPPERCASE = Regex("[A-Z]")
    }
}
