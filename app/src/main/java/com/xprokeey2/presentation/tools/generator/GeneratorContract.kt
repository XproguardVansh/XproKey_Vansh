package com.xprokeey2.presentation.tools.generator

import com.xprokeey2.domain.usecase.vault.GeneratedPasswordStrength
import com.xprokeey2.domain.usecase.vault.PasswordGeneratorOptions
import com.xprokeey2.presentation.workspace.UserBadge

/** The web page's length slider: 4 to 40, starting at 20. */
const val MIN_LENGTH = 4
const val MAX_LENGTH = 40
const val DEFAULT_LENGTH = 20

data class GeneratorUiState(
    val user: UserBadge? = null,
    val options: PasswordGeneratorOptions = PasswordGeneratorOptions(length = DEFAULT_LENGTH),
    /** "" when no character type is selected. */
    val password: String = "",
) {
    val entropyBits: Int get() = GeneratedPasswordStrength.entropyBits(password, options.charsetSize)
    val strength: GeneratedPasswordStrength get() = GeneratedPasswordStrength.of(entropyBits)
}

sealed interface GeneratorAction {
    /** Any option changed: a new password is made, like on the web. */
    data class OptionsChanged(val options: PasswordGeneratorOptions) : GeneratorAction

    data object Regenerate : GeneratorAction
}
