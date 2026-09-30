package com.xprokeey2.domain.usecase.theme

import com.xprokeey2.domain.model.ThemeMode
import com.xprokeey2.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = repository.observeThemeMode()
}
