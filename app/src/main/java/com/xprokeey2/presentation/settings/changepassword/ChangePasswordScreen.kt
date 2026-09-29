package com.xprokeey2.presentation.settings.changepassword

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.UiText
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
fun ChangePasswordScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onPasswordChanged: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChangePasswordEvent.ShowMessage -> scope.launch {
                // Each tap can bring a new error: show the latest one instead of queueing them.
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is ChangePasswordEvent.PasswordChanged -> onPasswordChanged(event.message.asString(context))
            is ChangePasswordEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    ChangePasswordScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onSectionClick = onSectionClick,
    )
}

/** Settings > Change password, laid out like the web page. */
@Composable
fun ChangePasswordScreen(
    state: ChangePasswordUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (ChangePasswordAction) -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.CHANGE_PASSWORD,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.change_password_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.change_password_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                if (state.isOtpSent) {
                    VerifyOtpStep(state = state, onAction = onAction)
                } else {
                    RequestOtpStep(state = state, onAction = onAction)
                }
            }
        }
    }
}

/** Step 1: the OTP goes to the account's email, shown read-only. */
@Composable
private fun RequestOtpStep(state: ChangePasswordUiState, onAction: (ChangePasswordAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        StepHeading(
            icon = R.drawable.ic_lock,
            title = stringResource(R.string.change_password_request_otp),
            body = stringResource(R.string.change_password_request_otp_body),
        )
        XpTextField(
            value = state.email,
            onValueChange = {},
            label = stringResource(R.string.change_password_email_label),
            labelIcon = R.drawable.ic_mail,
            supportingText = stringResource(R.string.change_password_email_hint),
            enabled = false,
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { ErrorBox(it.asString()) }
        FormButton(
            text = stringResource(R.string.change_password_send_otp),
            loadingText = stringResource(R.string.change_password_sending_otp),
            isLoading = state.isSendingOtp,
            enabled = state.email.isNotBlank(),
            onClick = { onAction(ChangePasswordAction.SendOtp) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Step 2: the emailed OTP and the new password. */
@Composable
private fun VerifyOtpStep(state: ChangePasswordUiState, onAction: (ChangePasswordAction) -> Unit) {
    val colors = XpTheme.colors
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(ChangePasswordAction.Submit)
    }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        StepHeading(
            icon = R.drawable.ic_key_round,
            title = stringResource(R.string.change_password_verify),
            body = stringResource(R.string.change_password_verify_body),
        )
        XpTextField(
            value = state.otp,
            onValueChange = { onAction(ChangePasswordAction.OtpChanged(it)) },
            label = stringResource(R.string.change_password_otp_label),
            placeholder = stringResource(R.string.change_password_otp_placeholder),
            supportingText = stringResource(R.string.change_password_otp_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(ChangePasswordAction.PasswordChanged(it)) },
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(ChangePasswordAction.TogglePasswordVisibility) },
            label = stringResource(R.string.change_password_new_label),
            placeholder = stringResource(R.string.change_password_new_placeholder),
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth(),
        )
        XpPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(ChangePasswordAction.ConfirmPasswordChanged(it)) },
            isPasswordVisible = state.isConfirmPasswordVisible,
            onToggleVisibility = { onAction(ChangePasswordAction.ToggleConfirmPasswordVisibility) },
            label = stringResource(R.string.change_password_confirm_label),
            placeholder = stringResource(R.string.change_password_confirm_placeholder),
            contentType = ContentType.NewPassword,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { ErrorBox(it.asString()) }
        Column {
            HorizontalDivider(color = colors.divider)
            Spacer(Modifier.height(16.dp))
            // "Back" is short: the wider right button keeps "Change Password" on one line on phones.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XpSecondaryButton(
                    text = stringResource(R.string.back),
                    onClick = { onAction(ChangePasswordAction.Back) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                )
                FormButton(
                    text = stringResource(R.string.change_password_submit),
                    loadingText = stringResource(R.string.change_password_submitting),
                    isLoading = state.isSubmitting,
                    icon = R.drawable.ic_save,
                    onClick = submit,
                    modifier = Modifier.weight(1.75f),
                )
            }
        }
    }
}

/** "REQUEST OTP" / "VERIFY OTP & SET NEW PASSWORD" and the step's explanation. */
@Composable
private fun StepHeading(@DrawableRes icon: Int, title: String, body: String) {
    val colors = XpTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title.uppercase(),
                style = XpTheme.typography.fieldLabel,
                color = colors.textLabel,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = body,
            style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 19.sp),
            color = colors.textSecondary,
        )
    }
}

/** The web's red message box above the buttons. */
@Composable
private fun ErrorBox(message: String) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = message,
        style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
        color = colors.error,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.error.copy(alpha = if (colors.isDark) 0.1f else 0.05f))
            .border(1.dp, colors.error.copy(alpha = if (colors.isDark) 0.3f else 0.15f), shape)
            .padding(16.dp),
    )
}

/** Blue button of the page: while [isLoading] it shows a spinner and [loadingText] ("Sending OTP…"). */
@Composable
private fun FormButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val colors = XpTheme.colors
    val clickable = enabled && !isLoading
    Row(
        modifier = modifier
            .height(44.dp)
            .alpha(if (clickable) 1f else 0.5f)
            .clip(ButtonShape)
            .background(colors.primary)
            .clickable(enabled = clickable, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = if (isLoading) loadingText else text,
            style = XpTheme.typography.button.copy(fontSize = 13.sp, lineHeight = 15.sp),
            color = colors.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RequestOtpPreview() {
    XproKeyTheme(darkTheme = true) {
        ChangePasswordScreen(
            state = ChangePasswordUiState(
                user = UserBadge.from(name = "Vansh Goel", email = "goelv2610@gmail.com"),
                email = "goelv2610@gmail.com",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onSectionClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun VerifyOtpPreview() {
    XproKeyTheme(darkTheme = false) {
        ChangePasswordScreen(
            state = ChangePasswordUiState(
                email = "goelv2610@gmail.com",
                isOtpSent = true,
                otp = "123",
                password = "secret-pass",
                error = UiText.Resource(R.string.change_password_otp_length),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onSectionClick = {},
        )
    }
}
