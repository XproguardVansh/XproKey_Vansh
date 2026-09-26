package com.xprokeey2.presentation.onboarding.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.license.ActivateLicenseUseCase
import com.xprokeey2.domain.usecase.validation.ValidateLicenseKeyUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.asUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivateLicenseViewModel @Inject constructor(
    private val activateLicense: ActivateLicenseUseCase,
    private val validateLicenseKey: ValidateLicenseKeyUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ActivateLicenseUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<ActivateLicenseEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ActivateLicenseAction) {
        when (action) {
            is ActivateLicenseAction.LicenseKeyChanged -> _state.update {
                it.copy(licenseKey = action.licenseKey, licenseKeyError = null)
            }
            ActivateLicenseAction.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return

        validateLicenseKey(current.licenseKey)?.let { error ->
            _state.update { it.copy(licenseKeyError = error.asUiText()) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = activateLicense(current.licenseKey)
            _state.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> _events.send(ActivateLicenseEvent.Activated(result.data.organization))
                is Resource.Error -> when (val error = result.error) {
                    DataError.SessionExpired -> _events.send(ActivateLicenseEvent.SessionExpired(error.asUiText()))
                    // e.g. invalid or already-used key: shown under the field.
                    else -> _state.update { it.copy(licenseKeyError = error.asUiText()) }
                }
            }
        }
    }
}
