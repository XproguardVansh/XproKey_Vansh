package com.xprokeey2.presentation.session

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.usecase.auth.LockVaultUseCase
import com.xprokeey2.domain.usecase.auth.SignOutUseCase
import com.xprokeey2.domain.usecase.security.ObserveSessionTimeoutUseCase
import com.xprokeey2.domain.usecase.security.SyncSessionTimeoutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The web's useSessionTimeout, run while a workspace screen is shown (the web runs it in the signed-in
 * layout): after the chosen minutes without a touch, the vault is locked or the user is logged out.
 * Time in the background counts too, like a browser tab's timer that keeps running.
 */
@Singleton
class SessionTimeoutMonitor @Inject constructor(
    observeSettings: ObserveSessionTimeoutUseCase,
    private val syncSettings: SyncSessionTimeoutUseCase,
    private val lockVault: LockVaultUseCase,
    private val signOut: SignOutUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var settings = SessionTimeoutSettings()
    private var workspaceScreens = 0
    private var lastActivityAt = 0L
    private var timedOut = false
    private var timer: Job? = null

    private val _timeouts = Channel<TimeoutAction>(Channel.CONFLATED)

    /** What happened when the timeout hit, for the app to open the Lock screen or Login. */
    val timeouts: Flow<TimeoutAction> = _timeouts.receiveAsFlow()

    init {
        scope.launch {
            observeSettings().collect {
                settings = it
                restartTimer()
            }
        }
    }

    /** A workspace screen appeared. Entering the signed-in app also copies the server's settings, like the web layout. */
    fun enterWorkspace() {
        workspaceScreens++
        if (workspaceScreens == 1) {
            timedOut = false
            lastActivityAt = now()
            restartTimer()
            scope.launch { syncSettings() }
        }
    }

    fun leaveWorkspace() {
        workspaceScreens = (workspaceScreens - 1).coerceAtLeast(0)
        if (workspaceScreens == 0) cancelTimer()
    }

    /** Any touch or key press (the web resets on mouse, keyboard, touch and scroll events). */
    fun onUserActivity() {
        if (workspaceScreens == 0 || timedOut) return
        lastActivityAt = now()
        restartTimer()
    }

    /** Back in the foreground: a sleeping device may have held the timer back. */
    fun onAppForeground() {
        if (workspaceScreens == 0 || timedOut) return
        val deadline = timeoutDeadline(lastActivityAt, settings.duration) ?: return
        if (now() >= deadline) timeOut()
    }

    private fun restartTimer() {
        cancelTimer()
        if (workspaceScreens == 0 || timedOut) return
        val deadline = timeoutDeadline(lastActivityAt, settings.duration) ?: return
        timer = scope.launch {
            delay((deadline - now()).coerceAtLeast(0))
            timer = null
            timeOut()
        }
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    /** The key or the whole session is cleared right away, even in the background; the screen follows. */
    private fun timeOut() {
        cancelTimer()
        timedOut = true
        val action = settings.action
        scope.launch {
            when (action) {
                TimeoutAction.LOCK -> lockVault()
                TimeoutAction.LOGOUT -> signOut()
            }
            _timeouts.send(action)
        }
    }

    private fun now(): Long = SystemClock.elapsedRealtime()
}

/** When the timeout hits after a last touch at [lastActivityAt] (ms); null for "Never". */
internal fun timeoutDeadline(lastActivityAt: Long, duration: TimeoutDuration): Long? =
    duration.minutes?.let { lastActivityAt + it * 60_000L }

/** Provided by MainActivity; null in previews, where screens just don't time out. */
val LocalSessionTimeoutMonitor = staticCompositionLocalOf<SessionTimeoutMonitor?> { null }

/** Runs the session timeout while the calling screen is shown. */
@Composable
fun SessionTimeoutEffect() {
    val monitor = LocalSessionTimeoutMonitor.current ?: return
    DisposableEffect(monitor) {
        monitor.enterWorkspace()
        onDispose { monitor.leaveWorkspace() }
    }
}
