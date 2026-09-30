package com.xprokeey2.presentation.settings.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.subscription.CancelSubscriptionUseCase
import com.xprokeey2.domain.usecase.subscription.GetBillingUseCase
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import com.xprokeey2.presentation.util.UiText
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
class BillingViewModel @Inject constructor(
    private val getBilling: GetBillingUseCase,
    private val cancelSubscription: CancelSubscriptionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BillingUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<BillingEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = getBilling()) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, billing = result.data) }
                is Resource.Error -> _events.send(
                    when (result.error) {
                        DataError.SessionExpired -> BillingEvent.SignInRequired(result.error.asUiText())
                        else -> BillingEvent.LoadFailed(result.error.loadFailedMessage())
                    }
                )
            }
        }
    }

    fun onAction(action: BillingAction) {
        when (action) {
            BillingAction.CancelSubscriptionClicked -> _state.update { it.copy(isCancelDialogVisible = true) }
            BillingAction.DismissCancelDialog -> _state.update { it.copy(isCancelDialogVisible = false) }
            BillingAction.ConfirmCancel -> cancel()
        }
    }

    private fun cancel() {
        if (_state.value.isCancelling) return
        _state.update { it.copy(isCancelDialogVisible = false, isCancelling = true) }

        viewModelScope.launch {
            val result = cancelSubscription()
            _state.update { it.copy(isCancelling = false) }
            when (result) {
                is Resource.Success -> if (result.data.isCancelled) {
                    _events.send(BillingEvent.ShowMessage(UiText.Resource(R.string.billing_cancel_success)))
                    reload()
                }
                is Resource.Error -> {
                    val error = result.error
                    when {
                        error == DataError.SessionExpired -> _events.send(BillingEvent.SignInRequired(error.asUiText()))
                        error == DataError.NoInternet || error == DataError.Timeout ->
                            _events.send(BillingEvent.ShowMessage(error.asUiText()))
                        // Razorpay stopped renewal but the server's record didn't follow (Razorpay guide 7.2).
                        error is DataError.Server && error.message.contains(RECORD_NOT_UPDATED, ignoreCase = true) -> {
                            _events.send(BillingEvent.ShowMessage(UiText.Resource(R.string.billing_cancel_record_lagging)))
                            reload()
                        }
                        else -> _events.send(BillingEvent.ShowMessage(UiText.Resource(R.string.billing_cancel_failed)))
                    }
                }
            }
        }
    }

    /** After cancelling, like the web's second loadBilling(); a failure keeps what's shown. */
    private suspend fun reload() {
        val result = getBilling()
        if (result is Resource.Success) _state.update { it.copy(billing = result.data) }
    }

    private companion object {
        const val RECORD_NOT_UPDATED = "failed to update local record"
    }
}

/** The web's `error || message || "Failed to load billing information."`. */
private fun DataError.loadFailedMessage(): UiText = when (this) {
    is DataError.Server -> message.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
        ?: UiText.Resource(R.string.billing_load_failed)
    DataError.NoInternet, DataError.Timeout -> asUiText()
    else -> UiText.Resource(R.string.billing_load_failed)
}
