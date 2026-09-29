package com.xprokeey2.presentation.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * Shows [message] handed over by the screen the user just left (e.g. "Card saved") once.
 * [onShown] clears it right away, and the new key cancels this effect, so the snackbar runs in
 * the screen's scope: in the effect it would be removed as soon as it appeared.
 */
@Composable
fun ResultMessageEffect(message: String?, snackbarHostState: SnackbarHostState, onShown: () -> Unit) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(message) {
        if (message != null) {
            onShown()
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    }
}
