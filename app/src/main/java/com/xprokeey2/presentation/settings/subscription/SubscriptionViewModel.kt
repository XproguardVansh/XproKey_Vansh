package com.xprokeey2.presentation.settings.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.usecase.subscription.GetSubscriptionOverviewUseCase
import com.xprokeey2.domain.usecase.user.GetSignedInUserUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.asUiText
import com.xprokeey2.presentation.workspace.toBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    getSignedInUser: GetSignedInUserUseCase,
    private val getOverview: GetSubscriptionOverviewUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<SubscriptionEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getSignedInUser()?.toBadge()
            _state.update { it.copy(user = user) }
        }
    }

    /** Each time the screen shows, like the web page loading again, e.g. back from Manage Subscription. */
    fun refresh() {
        viewModelScope.launch {
            when (val result = getOverview()) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, overview = result.data) }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false) }
                    _events.send(
                        when (result.error) {
                            DataError.SessionExpired -> SubscriptionEvent.SignInRequired(result.error.asUiText())
                            else -> SubscriptionEvent.ShowMessage(result.error.asUiText())
                        }
                    )
                }
            }
        }
    }
}
