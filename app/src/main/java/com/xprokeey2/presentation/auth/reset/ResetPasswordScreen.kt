package com.xprokeey2.presentation.auth.reset

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
import androidx.compose.ui.focus.FocusDirection
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
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreenRoot(
    onPasswordReset: (email: String, message: String) -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ResetPasswordEvent.PasswordReset -> onPasswordReset(event.email, event.message)
            is ResetPasswordEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    ResetPasswordScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ResetPasswordAction) -> Unit,
    onBackToLogin: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val moveFocusDown = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val submit = {
        focusManager.clearFocus()
        onAction(ResetPasswordAction.Submit)
    }

    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        XpLogoHeader()

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.reset_title),
            style = typography.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.reset_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        XpTextField(
            value = state.email,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        XpTextField(
            value = state.otp,
            onValueChange = { onAction(ResetPasswordAction.OtpChanged(it)) },
            placeholder = stringResource(R.string.placeholder_enter_otp),
            supportingText = stringResource(R.string.otp_valid_hint),
            error = state.otpError?.asString(),
            enabled = !state.isLoading,
            contentType = ContentType.SmsOtpCode,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            keyboardActions = moveFocusDown,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.requiresRecoveryKey) {
            Spacer(Modifier.height(14.dp))
            XpTextField(
                value = state.recoveryKey,
                onValueChange = { onAction(ResetPasswordAction.RecoveryKeyChanged(it)) },
                placeholder = stringResource(R.string.placeholder_recovery_key),
                supportingText = stringResource(R.string.recovery_key_hint),
                error = state.recoveryKeyError?.asString(),
                enabled = !state.isLoading,
                // Password keyboard type: stays visible on screen but the keyboard won't learn it.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = moveFocusDown,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))
        XpPasswordField(
            value = state.newPassword,
            onValueChange = { onAction(ResetPasswordAction.NewPasswordChanged(it)) },
            placeholder = stringResource(R.string.placeholder_new_password),
            isPasswordVisible = state.isNewPasswordVisible,
            onToggleVisibility = { onAction(ResetPasswordAction.ToggleNewPasswordVisibility) },
            error = state.newPasswordError?.asString(),
            enabled = !state.isLoading,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Next,
            keyboardActions = moveFocusDown,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        XpPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(ResetPasswordAction.ConfirmPasswordChanged(it)) },
            placeholder = stringResource(R.string.placeholder_confirm_new_password),
            isPasswordVisible = state.isConfirmPasswordVisible,
            onToggleVisibility = { onAction(ResetPasswordAction.ToggleConfirmPasswordVisibility) },
            error = state.confirmPasswordError?.asString(),
            enabled = !state.isLoading,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        XpPrimaryButton(
            text = stringResource(R.string.reset_password),
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

@Preview(name = "Dark", showBackground = true, heightDp = 900)
@Composable
private fun ResetPasswordScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        ResetPasswordScreen(
            state = ResetPasswordUiState(email = "goelv2610@gmail.com", requiresRecoveryKey = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBackToLogin = {},
        )
    }
}
