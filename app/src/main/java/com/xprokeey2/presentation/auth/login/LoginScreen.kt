package com.xprokeey2.presentation.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.OrDivider
import com.xprokeey2.presentation.components.XpCheckbox
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun LoginScreenRoot(
    onNavigateToSignup: () -> Unit,
    onNavigateToVerify: (email: String) -> Unit,
    onNavigateToForgotPassword: (email: String) -> Unit,
    onNavigateToAccountSetup: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.NavigateToVerify -> onNavigateToVerify(event.email)
            LoginEvent.NavigateToAccountSetup -> onNavigateToAccountSetup()
            LoginEvent.NavigateToDashboard -> onNavigateToDashboard()
            is LoginEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    LoginScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onForgotPasswordClick = { onNavigateToForgotPassword(state.email.trim()) },
        onGoogleClick = {}, // No Google-login API yet.
        onCreateAccountClick = onNavigateToSignup,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (LoginAction) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current

    AuthScreenLayout(snackbarHostState = snackbarHostState) {
        XpLogoHeader()

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.login_title),
            style = typography.headline,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(28.dp))
        XpTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            label = stringResource(R.string.label_email),
            placeholder = stringResource(R.string.placeholder_email),
            error = state.emailError?.asString(),
            enabled = !state.isLoading,
            contentType = ContentType.EmailAddress + ContentType.Username,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = stringResource(R.string.label_master_password),
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(LoginAction.TogglePasswordVisibility) },
            error = state.passwordError?.asString(),
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onAction(LoginAction.Submit)
            }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            XpCheckbox(
                checked = state.rememberMe,
                onCheckedChange = { onAction(LoginAction.RememberMeChanged(it)) },
            ) {
                Text(
                    text = stringResource(R.string.remember_me),
                    style = typography.body,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = stringResource(R.string.forgot_password),
                style = typography.bodyBold,
                color = colors.primary,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onForgotPasswordClick)
                    .padding(vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        XpPrimaryButton(
            text = stringResource(R.string.sign_in),
            onClick = {
                focusManager.clearFocus()
                onAction(LoginAction.Submit)
            },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        OrDivider(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        XpSecondaryButton(
            text = stringResource(R.string.continue_with_google),
            onClick = onGoogleClick,
            leadingIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.new_to_xprokey))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary)) {
                    append(stringResource(R.string.create_an_account))
                }
            },
            style = typography.body,
            color = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(role = Role.Button, onClick = onCreateAccountClick)
                .padding(8.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun LoginScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        LoginScreen(
            state = LoginUiState(email = "aarav.m@xprokey.app", password = "secret-pass"),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onForgotPasswordClick = {},
            onGoogleClick = {},
            onCreateAccountClick = {},
        )
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun LoginScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        LoginScreen(
            state = LoginUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onForgotPasswordClick = {},
            onGoogleClick = {},
            onCreateAccountClick = {},
        )
    }
}
