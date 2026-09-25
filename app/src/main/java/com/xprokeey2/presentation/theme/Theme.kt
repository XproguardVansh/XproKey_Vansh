package com.xprokeey2.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalXpColors = staticCompositionLocalOf { LightXpColors }
private val LocalXpTypography = staticCompositionLocalOf { XpTypography() }

object XpTheme {
    val colors: XpColors
        @Composable @ReadOnlyComposable get() = LocalXpColors.current

    val typography: XpTypography
        @Composable @ReadOnlyComposable get() = LocalXpTypography.current
}

@Composable
fun XproKeyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkXpColors else LightXpColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.fieldBorder,
            error = colors.error,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.fieldBorder,
            error = colors.error,
        )
    }

    CompositionLocalProvider(
        LocalXpColors provides colors,
        LocalXpTypography provides XpTypography(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            content = content,
        )
    }
}
