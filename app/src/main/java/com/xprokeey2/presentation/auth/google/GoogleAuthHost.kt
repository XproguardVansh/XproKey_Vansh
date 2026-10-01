package com.xprokeey2.presentation.auth.google

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.verify.components.RecoveryKeyDialog
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

/**
 * Google sign-in for the Login and Sign up screens, like the web's GoogleAuthButton. [content] draws
 * the screen and gets the Google button's state; this adds Google's account picker and the web's
 * dialogs (create master password, unlock vault, recovery key). [onSignedIn] gets the same answer as a
 * password login: first-time account setup, or straight into the app with [message] to show.
 */
@Composable
fun GoogleAuthHost(
    snackbarHostState: SnackbarHostState,
    onSignedIn: (needsAccountSetup: Boolean, message: String?) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    viewModel: GoogleAuthViewModel = hiltViewModel(),
    content: @Composable (isGoogleBusy: Boolean, onGoogleClick: () -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is GoogleAuthEvent.SignedIn -> onSignedIn(event.needsAccountSetup, event.message?.asString(context))
            is GoogleAuthEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is GoogleAuthEvent.OpenForgotPassword -> onForgotPassword(event.email)
        }
    }

    content(state.isAuthenticating) {
        if (!state.isAuthenticating) {
            viewModel.onAction(GoogleAuthAction.PickerOpened)
            scope.launch {
                // Closing the screen mid-pick counts as cancelled, so the button doesn't stay busy.
                var result: GoogleIdTokenResult = GoogleIdTokenResult.Cancelled
                try {
                    result = requestGoogleIdToken(context)
                } finally {
                    viewModel.onAction(GoogleAuthAction.PickerClosed(result))
                }
            }
        }
    }

    state.setup?.let { VaultSetupDialog(state = it, onAction = viewModel::onAction) }
    state.unlock?.let { VaultUnlockDialog(state = it, onAction = viewModel::onAction) }
    state.recovery?.let {
        RecoveryKeyDialog(
            email = it.email,
            recoveryKey = it.recoveryKey,
            onContinue = { viewModel.onAction(GoogleAuthAction.RecoveryKeySaved) },
        )
    }
}

/** The web's Google button: Google's logo and [text]; while [isBusy], a spinner and "Authenticating with Google...". */
@Composable
fun GoogleSignInButton(
    text: String,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    XpSecondaryButton(
        text = if (isBusy) stringResource(R.string.google_authenticating) else text,
        onClick = onClick,
        enabled = !isBusy,
        leadingIcon = {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = XpTheme.colors.textSecondary,
                    strokeWidth = 2.dp,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        modifier = modifier,
    )
}
