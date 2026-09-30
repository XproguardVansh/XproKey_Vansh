package com.xprokeey2.presentation.settings.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.usecase.security.GetSecuritySettingsUseCase
import com.xprokeey2.domain.usecase.security.SaveSecuritySettingsUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.util.asUiText
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    getSignedInUser: GetSignedInUserUseCase,
    private val getSettings: GetSecuritySettingsUseCase,
    private val saveSettings: SaveSecuritySettingsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<SecurityEvent>()
    val events = _events.receiveAsFlow()

    private var savedBadgeJob: Job? = null

    init {
        viewModelScope.launch {
            val user = getSignedInUser()?.toBadge()
            _state.update { it.copy(user = user) }
        }
        viewModelScope.launch {
            when (val result = getSettings()) {
                is Resource.Success -> _state.update {
                    it.copy(isLoading = false, duration = result.data.duration, action = result.data.action)
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(SecurityEvent.SignInRequired(result.error.asUiText()))
                }
            }
        }
    }

    fun onAction(action: SecurityAction) {
        when (action) {
            is SecurityAction.DurationSelected -> _state.update { it.copy(duration = action.duration) }
            is SecurityAction.ActionSelected -> _state.update { it.copy(action = action.action) }
            SecurityAction.Save -> save()
        }
    }

    private fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val result = saveSettings(SessionTimeoutSettings(duration = current.duration, action = current.action))
            _state.update { it.copy(isSaving = false) }
            when (result) {
                is Resource.Success -> {
                    showSavedBadge()
                    _events.send(SecurityEvent.ShowMessage(UiText.Resource(R.string.security_saved_message)))
                }
                is Resource.Error -> _events.send(
                    when (result.error) {
                        DataError.SessionExpired -> SecurityEvent.SignInRequired(result.error.asUiText())
                        else -> SecurityEvent.ShowMessage(UiText.Resource(R.string.security_save_failed))
                    }
                )
            }
        }
    }

    private fun showSavedBadge() {
        savedBadgeJob?.cancel()
        _state.update { it.copy(isSaved = true) }
        savedBadgeJob = viewModelScope.launch {
            delay(SAVED_BADGE_MILLIS)
            _state.update { it.copy(isSaved = false) }
        }
    }

    private companion object {
        const val SAVED_BADGE_MILLIS = 2_500L
    }
}
