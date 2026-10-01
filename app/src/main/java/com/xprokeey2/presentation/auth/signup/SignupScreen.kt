package com.xprokeey2.presentation.auth.signup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.PasswordStrength
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.auth.google.GoogleAuthHost
import com.xprokeey2.presentation.auth.google.GoogleSignInButton
import com.xprokeey2.presentation.components.OrDivider
import com.xprokeey2.presentation.components.PasswordStrengthMeter
import com.xprokeey2.presentation.components.XpCheckbox
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

@Composable
fun SignupScreenRoot(
    onNavigateToLogin: () -> Unit,
    onNavigateToVerify: (email: String) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    /** Google sign-up ends signed in, like a login: first-time account setup or the Dashboard. */
    onNavigateToAccountSetup: () -> Unit,
    onNavigateToDashboard: (message: String?) -> Unit,
    onNavigateToForgotPassword: (email: String) -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SignupEvent.NavigateToVerify -> onNavigateToVerify(event.email)
            is SignupEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
        }
    }

    GoogleAuthHost(
        snackbarHostState = snackbarHostState,
        onSignedIn = { needsAccountSetup, message ->
            if (needsAccountSetup) onNavigateToAccountSetup() else onNavigateToDashboard(message)
        },
        onForgotPassword = onNavigateToForgotPassword,
    ) { isGoogleBusy, onGoogleClick ->
        SignupScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onAction = viewModel::onAction,
            isGoogleBusy = isGoogleBusy,
            onGoogleClick = onGoogleClick,
            onSignInClick = onNavigateToLogin,
            onTermsClick = onOpenTerms,
            onPrivacyPolicyClick = onOpenPrivacyPolicy,
        )
    }
}

@Composable
fun SignupScreen(
    state: SignupUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (SignupAction) -> Unit,
    onGoogleClick: () -> Unit,
    onSignInClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    isGoogleBusy: Boolean = false,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val moveFocusDown = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

    AuthScreenLayout(snackbarHostState = snackbarHostState) {
        XpLogoHeader()

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.signup_title),
            style = typography.headline,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.signup_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(24.dp))
        XpTextField(
            value = state.name,
            onValueChange = { onAction(SignupAction.NameChanged(it)) },
            label = stringResource(R.string.label_full_name),
            placeholder = stringResource(R.string.placeholder_full_name),
            error = state.nameError?.asString(),
            enabled = !state.isLoading,
            required = true,
            contentType = ContentType.PersonFullName,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = moveFocusDown,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        XpTextField(
            value = state.email,
            onValueChange = { onAction(SignupAction.EmailChanged(it)) },
            label = stringResource(R.string.label_email),
            placeholder = stringResource(R.string.placeholder_email),
            error = state.emailError?.asString(),
            enabled = !state.isLoading,
            required = true,
            contentType = ContentType.EmailAddress + ContentType.NewUsername,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            keyboardActions = moveFocusDown,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(SignupAction.PasswordChanged(it)) },
            label = stringResource(R.string.label_master_password),
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(SignupAction.TogglePasswordVisibility) },
            error = state.passwordError?.asString(),
            enabled = !state.isLoading,
            required = true,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Next,
            keyboardActions = moveFocusDown,
            modifier = Modifier.fillMaxWidth(),
        )
        state.passwordStrength?.let { strength ->
            Spacer(Modifier.height(10.dp))
            PasswordStrengthMeter(
                strength = strength,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
        XpPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(SignupAction.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.label_confirm_master_password),
            isPasswordVisible = state.isConfirmPasswordVisible,
            onToggleVisibility = { onAction(SignupAction.ToggleConfirmPasswordVisibility) },
            error = state.confirmPasswordError?.asString(),
            enabled = !state.isLoading,
            required = true,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onAction(SignupAction.Submit)
            }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        XpCheckbox(
            checked = state.acceptedTerms,
            onCheckedChange = { onAction(SignupAction.AcceptedTermsChanged(it)) },
            isError = state.termsError != null,
        ) {
            val linkStyle = TextLinkStyles(SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.i_agree_to_the))
                    append(" ")
                    withLink(LinkAnnotation.Clickable("terms", linkStyle) { onTermsClick() }) {
                        append(stringResource(R.string.terms))
                    }
                    append(" ")
                    append(stringResource(R.string.ampersand))
                    append(" ")
                    withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onPrivacyPolicyClick() }) {
                        append(stringResource(R.string.privacy_policy))
                    }
                },
                style = typography.checkboxLabel,
                color = colors.textSecondary,
            )
        }
        state.termsError?.let { error ->
            Text(
                text = error.asString(),
                style = typography.body.copy(fontSize = 11.5.sp),
                color = colors.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        XpPrimaryButton(
            text = stringResource(R.string.create_account),
            onClick = {
                focusManager.clearFocus()
                onAction(SignupAction.Submit)
            },
            isLoading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        OrDivider(modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        GoogleSignInButton(
            text = stringResource(R.string.sign_up_with_google),
            isBusy = isGoogleBusy,
            onClick = onGoogleClick,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.already_have_account))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary)) {
                    append(stringResource(R.string.sign_in_link))
                }
            },
            style = typography.body,
            color = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(role = Role.Button, onClick = onSignInClick)
                .padding(8.dp),
        )
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 1000)
@Composable
private fun SignupScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        SignupScreen(
            state = SignupUiState(
                name = "Aarav Mehta",
                password = "Sup3rSecret!",
                passwordStrength = PasswordStrength(score = 4),
                acceptedTerms = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onGoogleClick = {},
            onSignInClick = {},
            onTermsClick = {},
            onPrivacyPolicyClick = {},
        )
    }
}

@Preview(name = "Dark", showBackground = true, heightDp = 1000)
@Composable
private fun SignupScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        SignupScreen(
            state = SignupUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onGoogleClick = {},
            onSignInClick = {},
            onTermsClick = {},
            onPrivacyPolicyClick = {},
        )
    }
}
