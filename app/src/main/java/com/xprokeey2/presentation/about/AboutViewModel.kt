package com.xprokeey2.presentation.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings > About pages show static content; this only provides the account for the top bar. */
@HiltViewModel
class AboutViewModel @Inject constructor(
    getSignedInUser: GetSignedInUserUseCase,
) : ViewModel() {

    private val _user = MutableStateFlow<UserBadge?>(null)
    val user = _user.asStateFlow()

    init {
        viewModelScope.launch { _user.value = getSignedInUser()?.toBadge() }
    }
}
