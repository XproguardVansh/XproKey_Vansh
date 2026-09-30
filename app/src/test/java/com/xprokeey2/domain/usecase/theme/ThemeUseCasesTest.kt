package com.xprokeey2.domain.usecase.theme

import com.xprokeey2.domain.model.ThemeMode
import com.xprokeey2.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The web's next-themes values and its header's sun/moon button. */
class ThemeUseCasesTest {

    private class FakeRepository(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeRepository {
        val mode = MutableStateFlow(initial)
        override fun observeThemeMode(): Flow<ThemeMode> = mode
        override suspend fun setThemeMode(mode: ThemeMode) {
            this.mode.value = mode
        }
    }

    @Test
    fun savedValuesMatchNextThemes() {
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.value })
        assertEquals(ThemeMode.DARK, ThemeMode.of("dark"))
        assertNull(ThemeMode.of("sepia"))
        assertNull(ThemeMode.of(null))
    }

    @Test
    fun systemFollowsThePhoneAndAChoiceOverridesIt() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
    }

    @Test
    fun everyTapSwitchesWhatIsShowing() = runBlocking {
        // Following a dark phone: the web's first tap would save "dark" and change nothing.
        val repository = FakeRepository(ThemeMode.SYSTEM)
        val toggle = ToggleThemeUseCase(repository)

        toggle(isDarkShown = true)
        assertEquals(ThemeMode.LIGHT, repository.mode.value)

        toggle(isDarkShown = false)
        assertEquals(ThemeMode.DARK, repository.mode.value)
    }
}
