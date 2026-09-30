package com.xprokeey2.domain.repository

import com.xprokeey2.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * This device's theme choice. The web keeps it in localStorage ("theme"), which logging out
 * doesn't clear, so it stays after signing out here too.
 */
interface ThemeRepository {

    /** The saved choice as it changes; [ThemeMode.SYSTEM] until one is saved. */
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
