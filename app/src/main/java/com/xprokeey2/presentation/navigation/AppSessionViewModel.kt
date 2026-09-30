package com.xprokeey2.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xprokeey2.domain.model.AccessBlock
import com.xprokeey2.domain.model.LaunchDestination
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.security.AccessGate
import com.xprokeey2.domain.usecase.auth.GetLaunchDestinationUseCase
import com.xprokeey2.domain.usecase.auth.LogOutUseCase
import com.xprokeey2.domain.usecase.auth.NeedsUnlockUseCase
import com.xprokeey2.domain.usecase.auth.SignOutUseCase
import com.xprokeey2.domain.usecase.subscription.ConfirmSubscriptionActiveUseCase
import com.xprokeey2.domain.usecase.subscription.RecheckPendingPaymentUseCase
import com.xprokeey2.presentation.session.SessionTimeoutMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/** App-wide moves that don't belong to one screen. */
sealed interface AppSessionEvent {
    /** The app was opened: show [destination] unless it's already on screen (e.g. restored after the system closed it). */
    data class Launched(val destination: LaunchDestination) : AppSessionEvent

    /** The server wants a payment ("payment" or "trial_expired"): open Choose your plan. */
    data object OpenCheckout : AppSessionEvent

    /** Business account without a license: open Activate license. */
    data object OpenLicenseActivation : AppSessionEvent

    /** The vault key isn't in memory (session timeout "Lock", or the app was reopened): the Lock screen asks for the master password. */
    data object Locked : AppSessionEvent

    /** Session timeout "Log out", or "Log out" in the account menu: the session is cleared. */
    data object SignedOut : AppSessionEvent

    /** The session is over (e.g. the server rejected it): it's cleared and Login shows [message]. */
    data class SignInRequired(val message: String) : AppSessionEvent
}

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    accessGate: AccessGate,
    getLaunchDestination: GetLaunchDestinationUseCase,
    private val sessionTimeout: SessionTimeoutMonitor,
    private val confirmSubscriptionActive: ConfirmSubscriptionActiveUseCase,
    private val recheckPendingPayment: RecheckPendingPaymentUseCase,
    private val logOutUseCase: LogOutUseCase,
    private val needsUnlock: NeedsUnlockUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    private val appEvents = Channel<AppSessionEvent>(Channel.BUFFERED)
    private val handlingBlock = Mutex()
    private var isLoggingOut = false

    private val _launchDestination = MutableStateFlow<LaunchDestination?>(null)

    /** Where the app opens; null for the few milliseconds it takes to decide (nothing is drawn meanwhile). */
    val launchDestination: StateFlow<LaunchDestination?> = _launchDestination.asStateFlow()

    val events: Flow<AppSessionEvent> = merge(
        appEvents.receiveAsFlow(),
        sessionTimeout.timeouts.map { action ->
            when (action) {
                TimeoutAction.LOCK -> AppSessionEvent.Locked
                TimeoutAction.LOGOUT -> AppSessionEvent.SignedOut
            }
        },
    )

    init {
        viewModelScope.launch {
            val destination = getLaunchDestination(nowMillis = System.currentTimeMillis())
            _launchDestination.value = destination
            appEvents.send(AppSessionEvent.Launched(destination))
        }
        // Like the web's API client. Parallel requests all get the 403, so one is handled at a time.
        viewModelScope.launch {
            accessGate.blocks.collect { block ->
                if (!handlingBlock.tryLock()) return@collect
                launch {
                    try {
                        handle(block)?.let { appEvents.send(it) }
                    } finally {
                        handlingBlock.unlock()
                    }
                }
            }
        }
    }

    /** The account menu's "Log out": the server is told, this device forgets the session, then Login. */
    fun logOut() {
        if (isLoggingOut) return
        isLoggingOut = true
        viewModelScope.launch {
            try {
                logOutUseCase()
                appEvents.send(AppSessionEvent.SignedOut)
            } finally {
                isLoggingOut = false
            }
        }
    }

    /**
     * A screen needs the user to sign in again. If only the vault key is missing (the app was reopened),
     * the Lock screen's master password is enough; otherwise the session is over and Login opens.
     */
    fun onSignInRequired(message: String) {
        viewModelScope.launch {
            if (needsUnlock()) {
                appEvents.send(AppSessionEvent.Locked)
            } else {
                signOut()
                appEvents.send(AppSessionEvent.SignInRequired(message))
            }
        }
    }

    /** Back in the foreground: the timeout may be due, and a pending payment may have gone through. */
    fun onAppForeground() {
        sessionTimeout.onAppForeground()
        viewModelScope.launch { recheckPendingPayment() }
    }

    /** Leaving the app: the last use is saved for the session timeout. */
    fun onAppBackground() = sessionTimeout.onAppBackground()

    private suspend fun handle(block: AccessBlock): AppSessionEvent? = when (block) {
        // A renewal or a new payment may not have reached the server yet (Razorpay guide 7.3).
        AccessBlock.PAYMENT -> if (confirmSubscriptionActive()) null else AppSessionEvent.OpenCheckout
        AccessBlock.TRIAL_EXPIRED -> AppSessionEvent.OpenCheckout
        AccessBlock.ACTIVATE_LICENSE -> AppSessionEvent.OpenLicenseActivation
    }
}
