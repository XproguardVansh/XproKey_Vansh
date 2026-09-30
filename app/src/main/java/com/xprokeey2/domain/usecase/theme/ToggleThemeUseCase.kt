package com.xprokeey2.domain.usecase.theme

import com.xprokeey2.domain.model.ThemeMode
import com.xprokeey2.domain.repository.ThemeRepository
import javax.inject.Inject

/**
 * The web header's sun/moon button: light when dark is showing, otherwise dark, and saved. The web
 * checks the saved value instead, so its first tap changes nothing while it follows a dark device;
 * here every tap switches what's on screen (the user's choice).
 */
class ToggleThemeUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    suspend operator fun invoke(isDarkShown: Boolean) {
        repository.setThemeMode(if (isDarkShown) ThemeMode.LIGHT else ThemeMode.DARK)
    }
}
