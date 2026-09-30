package com.xprokeey2.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.model.AccessBlock
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.security.AccessGate
import com.xprokeey2.domain.usecase.subscription.ConfirmSubscriptionActiveUseCase
import com.xprokeey2.domain.usecase.subscription.RecheckPendingPaymentUseCase
import com.xprokeey2.presentation.session.SessionTimeoutMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/** App-wide moves that don't belong to one screen. */
sealed interface AppSessionEvent {
    /** The server wants a payment ("payment" or "trial_expired"): open Choose your plan. */
    data object OpenCheckout : AppSessionEvent

    /** Business account without a license: open Activate license. */
    data object OpenLicenseActivation : AppSessionEvent

    /** Session timeout "Lock": the vault key is gone, the Lock screen asks for the master password. */
    data object Locked : AppSessionEvent

    /** Session timeout "Log out": the session is cleared. */
    data object SignedOut : AppSessionEvent
}

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    accessGate: AccessGate,
    private val sessionTimeout: SessionTimeoutMonitor,
    private val confirmSubscriptionActive: ConfirmSubscriptionActiveUseCase,
    private val recheckPendingPayment: RecheckPendingPaymentUseCase,
) : ViewModel() {

    private val accessEvents = Channel<AppSessionEvent>(Channel.BUFFERED)
    private val handlingBlock = Mutex()

    val events: Flow<AppSessionEvent> = merge(
        accessEvents.receiveAsFlow(),
        sessionTimeout.timeouts.map { action ->
            when (action) {
                TimeoutAction.LOCK -> AppSessionEvent.Locked
                TimeoutAction.LOGOUT -> AppSessionEvent.SignedOut
            }
        },
    )

    init {
        // Like the web's API client. Parallel requests all get the 403, so one is handled at a time.
        viewModelScope.launch {
            accessGate.blocks.collect { block ->
                if (!handlingBlock.tryLock()) return@collect
                launch {
                    try {
                        handle(block)?.let { accessEvents.send(it) }
                    } finally {
                        handlingBlock.unlock()
                    }
                }
            }
        }
    }

    /** Back in the foreground: the timeout may be due, and a pending payment may have gone through. */
    fun onAppForeground() {
        sessionTimeout.onAppForeground()
        viewModelScope.launch { recheckPendingPayment() }
    }

    private suspend fun handle(block: AccessBlock): AppSessionEvent? = when (block) {
        // A renewal or a new payment may not have reached the server yet (Razorpay guide 7.3).
        AccessBlock.PAYMENT -> if (confirmSubscriptionActive()) null else AppSessionEvent.OpenCheckout
        AccessBlock.TRIAL_EXPIRED -> AppSessionEvent.OpenCheckout
        AccessBlock.ACTIVATE_LICENSE -> AppSessionEvent.OpenLicenseActivation
    }
}
