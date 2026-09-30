package com.xprokeey2.presentation.auth.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.auth.SignOutUseCase
import com.xprokeey2.domain.usecase.auth.UnlockVaultResult
import com.xprokeey2.domain.usecase.auth.UnlockVaultUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    getSignedInUser: GetSignedInUserUseCase,
    private val unlockVault: UnlockVaultUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LockUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<LockEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val email = getSignedInUser()?.email.orEmpty()
            _state.update { it.copy(email = email) }
        }
    }

    fun onAction(action: LockAction) {
        when (action) {
            is LockAction.PasswordChanged -> _state.update { it.copy(password = action.password, error = null) }
            LockAction.TogglePasswordVisibility -> _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            LockAction.Unlock -> unlock()
            LockAction.LogOut -> logOut(message = null)
        }
    }

    private fun unlock() {
        val current = _state.value
        if (current.isUnlocking) return
        _state.update { it.copy(error = null) }
        if (current.password.isEmpty()) {
            _state.update { it.copy(error = UiText.Resource(R.string.lock_password_required)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUnlocking = true) }
            val result = unlockVault(current.password)
            _state.update { it.copy(isUnlocking = false) }
            when (result) {
                UnlockVaultResult.UNLOCKED -> _events.send(LockEvent.Unlocked(UiText.Resource(R.string.lock_unlocked)))
                UnlockVaultResult.WRONG_PASSWORD -> {
                    _state.update { it.copy(error = UiText.Resource(R.string.lock_incorrect_password)) }
                    _events.send(LockEvent.ShowMessage(UiText.Resource(R.string.lock_incorrect_password_short)))
                }
                UnlockVaultResult.SESSION_DATA_MISSING -> logOut(UiText.Resource(R.string.lock_session_missing))
            }
        }
    }

    /** "Log out instead", or nothing left on this device to unlock with. */
    private fun logOut(message: UiText?) {
        viewModelScope.launch {
            signOut()
            _events.send(LockEvent.SignedOut(message))
        }
    }
}
