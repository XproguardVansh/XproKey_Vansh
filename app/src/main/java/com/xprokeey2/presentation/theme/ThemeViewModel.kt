package com.xprokeey2.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.model.ThemeMode
import com.xprokeey2.domain.usecase.theme.ObserveThemeModeUseCase
import com.xprokeey2.domain.usecase.theme.ToggleThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The theme of the whole app (the web's next-themes provider), held by MainActivity. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    private val toggleTheme: ToggleThemeUseCase,
) : ViewModel() {

    /** Null until the saved choice has been read; nothing is drawn before then. */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun onToggleTheme(isDarkShown: Boolean) {
        viewModelScope.launch { toggleTheme(isDarkShown) }
    }
}

/**
 * Switches the app between light and dark, given whether dark is showing now (the top bar's
 * sun/moon button). Provided by MainActivity; previews get a no-op.
 */
val LocalThemeToggle = staticCompositionLocalOf<(isDarkShown: Boolean) -> Unit> { {} }
