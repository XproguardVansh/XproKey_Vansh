package com.xprokeey2.presentation.auth.forgot

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthLinkText
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.navigation.ResetPasswordRoute
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreenRoot(
    onNavigateToReset: (ResetPasswordRoute) -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ForgotPasswordEvent.NavigateToReset -> onNavigateToReset(
                ResetPasswordRoute(
                    email = event.email,
                    masterSalt = event.masterSalt,
                    encryptedVaultKeyRecovery = event.encryptedVaultKeyRecovery,
                )
            )
            is ForgotPasswordEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    ForgotPasswordScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ForgotPasswordAction) -> Unit,
    onBackToLogin: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(ForgotPasswordAction.Submit)
    }

    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        XpLogoHeader()

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.forgot_title),
            style = typography.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.forgot_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        XpTextField(
            value = state.email,
            onValueChange = { onAction(ForgotPasswordAction.EmailChanged(it)) },
            placeholder = stringResource(R.string.placeholder_email_address),
            error = state.emailError?.asString(),
            enabled = !state.isLoading,
            contentType = ContentType.EmailAddress + ContentType.Username,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        XpPrimaryButton(
            text = stringResource(R.string.send_otp),
            onClick = submit,
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        AuthLinkText(
            prefix = stringResource(R.string.remember_password),
            link = stringResource(R.string.log_in),
            onClick = onBackToLogin,
        )
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBackToLogin = {},
        )
    }
}
