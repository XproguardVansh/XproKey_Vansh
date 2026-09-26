package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

class ValidateLicenseKeyUseCase @Inject constructor() {

    /** Returns null when a key was entered. The format is up to the server (no pattern enforced). */
    operator fun invoke(keyCode: String): ValidationError? =
        if (keyCode.isBlank()) ValidationError.REQUIRED else null
}
