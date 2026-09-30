package com.xprokeey2.domain.model

/** The app's colour theme, saved as the web's next-themes value ("system", "light" or "dark"). */
enum class ThemeMode(val value: String) {
    /** Follow the phone's dark theme setting: the web's default until its theme button is used. */
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    /** Whether the app shows dark colours, given the phone's own setting. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** A saved value; null when missing or unknown. */
        fun of(value: String?): ThemeMode? = entries.firstOrNull { it.value == value }
    }
}
