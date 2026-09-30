package com.xprokeey2.presentation.auth.lock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.OrDivider
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.UiText
import kotlinx.coroutines.launch

@Composable
fun LockScreenRoot(
    onUnlocked: (message: String) -> Unit,
    onSignedOut: (message: String?) -> Unit,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LockEvent.Unlocked -> onUnlocked(event.message.asString(context))
            is LockEvent.SignedOut -> onSignedOut(event.message?.asString(context))
            is LockEvent.ShowMessage -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    LockScreen(state = state, snackbarHostState = snackbarHostState, onAction = viewModel::onAction)
}

/** The web's lock page: shown when the session timed out with "Lock". */
@Composable
fun LockScreen(
    state: LockUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (LockAction) -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val unlock = {
        focusManager.clearFocus()
        onAction(LockAction.Unlock)
    }

    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        XpLogoHeader()

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.lock_title),
            style = typography.headline.copy(fontSize = 24.sp, lineHeight = 30.sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.lock_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (state.email.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.email,
                style = typography.bodyBold.copy(fontSize = 12.sp),
                color = colors.primary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(24.dp))
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(LockAction.PasswordChanged(it)) },
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(LockAction.TogglePasswordVisibility) },
            placeholder = stringResource(R.string.lock_password_placeholder),
            error = state.error?.asString(),
            enabled = !state.isUnlocking,
            keyboardActions = KeyboardActions(onDone = { unlock() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        XpPrimaryButton(
            text = stringResource(R.string.lock_unlock),
            onClick = unlock,
            isLoading = state.isUnlocking,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        OrDivider(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !state.isUnlocking, role = Role.Button) { onAction(LockAction.LogOut) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_log_out),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.lock_log_out_instead),
                style = typography.bodyBold.copy(fontSize = 12.sp),
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LockScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        LockScreen(
            state = LockUiState(
                email = "goelv2610@gmail.com",
                password = "secret",
                error = UiText.Resource(R.string.lock_incorrect_password),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
        )
    }
}
