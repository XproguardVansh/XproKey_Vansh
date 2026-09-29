package com.xprokeey2.presentation.tools.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.usecase.vault.GeneratePasswordUseCase
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tools > Generator: everything happens on the phone, nothing is sent anywhere. */
@HiltViewModel
class GeneratorViewModel @Inject constructor(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val generatePassword: GeneratePasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GeneratorUiState())
    val state = _state.asStateFlow()

    init {
        regenerate()
        viewModelScope.launch {
            val user = getSignedInUser()
            _state.update { it.copy(user = user?.toBadge()) }
        }
    }

    fun onAction(action: GeneratorAction) {
        when (action) {
            is GeneratorAction.OptionsChanged -> {
                val length = action.options.length.coerceIn(MIN_LENGTH, MAX_LENGTH)
                _state.update { it.copy(options = action.options.copy(length = length)) }
                regenerate()
            }
            GeneratorAction.Regenerate -> regenerate()
        }
    }

    private fun regenerate() {
        _state.update { it.copy(password = generatePassword(it.options)) }
    }
}
