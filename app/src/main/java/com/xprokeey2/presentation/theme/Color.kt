package com.xprokeey2.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colour tokens taken from the Xprokey Figma file (light + dark variants of each screen). */
@Immutable
data class XpColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textLabel: Color,
    val textPlaceholder: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val fieldBackground: Color,
    val fieldBorder: Color,
    val divider: Color,
    val checkboxBorder: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean,
)

val LightXpColors = XpColors(
    background = Color(0xFFEEF2FB),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F1733),
    textSecondary = Color(0xFF5D6B94),
    textLabel = Color(0xFF8A96BD),
    textPlaceholder = Color(0xFFA3ACCA),
    textMuted = Color(0xFF9AA6CC),
    primary = Color(0xFF2549D6),
    onPrimary = Color(0xFFFFFFFF),
    fieldBackground = Color(0xFFF7F9FF),
    fieldBorder = Color(0xFFE2E8F7),
    divider = Color(0xFFE6EBF9),
    checkboxBorder = Color(0xFFD2DCF2),
    success = Color(0xFF1F9D5B),
    warning = Color(0xFFD9882B),
    error = Color(0xFFE05A5A),
    isDark = false,
)

val DarkXpColors = XpColors(
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    textPrimary = Color(0xFFE8ECF5),
    textSecondary = Color(0xFF9AA6C2),
    textLabel = Color(0xFF717C92),
    textPlaceholder = Color(0xFFA3ACCA),
    textMuted = Color(0xFF5C6678),
    primary = Color(0xFF5B82FF),
    onPrimary = Color(0xFFFFFFFF),
    fieldBackground = Color(0xFF10151E),
    fieldBorder = Color(0xFF272F3D),
    divider = Color(0xFF222936),
    checkboxBorder = Color(0xFF2C3445),
    success = Color(0xFF34D399),
    warning = Color(0xFFD9882B),
    error = Color(0xFFE05A5A),
    isDark = true,
)
