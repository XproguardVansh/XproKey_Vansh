package com.xprokeey2.domain.usecase.validation

import javax.inject.Inject

data class VaultItemFormErrors(
    val title: ValidationError? = null,
    val url: ValidationError? = null,
    val username: ValidationError? = null,
    val password: ValidationError? = null,
) {
    val hasErrors: Boolean get() = title != null || url != null || username != null || password != null
}

/**
 * Name, website URL, username and password are required, as marked (*) in the web form. Any
 * website address is accepted; one without http(s) opens as https, like the web.
 */
class ValidateVaultItemFormUseCase @Inject constructor() {

    operator fun invoke(title: String, url: String, username: String, password: String) = VaultItemFormErrors(
        title = if (title.isBlank()) ValidationError.REQUIRED else null,
        url = if (url.isBlank()) ValidationError.REQUIRED else null,
        username = if (username.isBlank()) ValidationError.REQUIRED else null,
        password = if (password.isEmpty()) ValidationError.REQUIRED else null,
    )
}
