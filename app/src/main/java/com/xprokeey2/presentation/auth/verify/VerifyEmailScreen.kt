package com.xprokeey2.presentation.auth.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.usecase.validation.ValidateOtpUseCase
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.auth.verify.components.RecoveryKeyDialog
import com.xprokeey2.presentation.components.OtpInput
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreenRoot(
    onNavigateToLogin: (email: String) -> Unit,
    onBackToSignup: () -> Unit,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is VerifyEmailEvent.NavigateToLogin -> onNavigateToLogin(event.email)
            is VerifyEmailEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    VerifyEmailScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBackToSignup = onBackToSignup,
    )

    state.recoveryKey?.let { key ->
        RecoveryKeyDialog(
            email = state.email,
            recoveryKey = key,
            onContinue = { viewModel.onAction(VerifyEmailAction.RecoveryKeySaved) },
        )
    }
}

@Composable
fun VerifyEmailScreen(
    state: VerifyEmailUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (VerifyEmailAction) -> Unit,
    onBackToSignup: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val badgeShape = RoundedCornerShape(16.dp)

    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        XpLogoHeader()

        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.primary.copy(alpha = 0.10f), badgeShape)
                .border(1.dp, colors.primary.copy(alpha = 0.22f), badgeShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield_check),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.verify_title),
            style = typography.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.verify_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.email,
            style = typography.subtitle.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
        OtpInput(
            value = state.otp,
            onValueChange = { onAction(VerifyEmailAction.OtpChanged(it)) },
            isError = state.otpError != null,
            enabled = !state.isVerifying,
            onDone = {
                if (state.canVerify) {
                    focusManager.clearFocus()
                    onAction(VerifyEmailAction.Verify)
                }
            },
        )
        state.otpError?.let { error ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = error.asString(),
                style = typography.body,
                color = colors.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))
        XpPrimaryButton(
            text = stringResource(R.string.verify_account),
            onClick = {
                focusManager.clearFocus()
                onAction(VerifyEmailAction.Verify)
            },
            enabled = state.otp.length == ValidateOtpUseCase.OTP_LENGTH,
            isLoading = state.isVerifying,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = colors.divider)

        Spacer(Modifier.height(20.dp))
        ResendRow(
            secondsLeft = state.resendSecondsLeft,
            isResending = state.isResending,
            canResend = state.canResend,
            onResend = { onAction(VerifyEmailAction.Resend) },
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onBackToSignup)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.back_to_sign_up),
                style = typography.bodyBold,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun ResendRow(
    secondsLeft: Int,
    isResending: Boolean,
    canResend: Boolean,
    onResend: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.didnt_receive_code),
            style = typography.body,
            color = colors.textSecondary,
        )
        Spacer(Modifier.width(4.dp))
        when {
            isResending -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(8.dp)
                    .size(14.dp),
                color = colors.primary,
                strokeWidth = 2.dp,
            )
            secondsLeft > 0 -> Text(
                text = stringResource(R.string.resend_in, secondsLeft),
                style = typography.body,
                color = colors.textLabel,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            else -> Text(
                text = stringResource(R.string.resend_otp),
                style = typography.bodyBold,
                color = colors.primary,
                modifier = Modifier
                    .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
                    .padding(8.dp),
            )
        }
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun VerifyEmailScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        VerifyEmailScreen(
            state = VerifyEmailUiState(email = "vansh@xproguard.com", otp = "12", resendSecondsLeft = 51),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBackToSignup = {},
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun VerifyEmailScreenLightPreview() {
    XproKeyTheme(darkTheme = false) {
        VerifyEmailScreen(
            state = VerifyEmailUiState(email = "vansh@xproguard.com"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBackToSignup = {},
        )
    }
}
