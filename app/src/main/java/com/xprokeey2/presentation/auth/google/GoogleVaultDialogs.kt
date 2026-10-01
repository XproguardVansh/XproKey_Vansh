package com.xprokeey2.presentation.auth.google

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xprokeey2.R
import com.xprokeey2.domain.model.MasterPasswordStrength
import com.xprokeey2.presentation.components.XpActionButton
import com.xprokeey2.presentation.components.XpPasswordField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.UiText

/** The web's "Create Your Master Password" dialog, for an account's first Google sign-in. */
@Composable
fun VaultSetupDialog(state: VaultSetupDialogState, onAction: (GoogleAuthAction) -> Unit) {
    Dialog(
        onDismissRequest = { onAction(GoogleAuthAction.DismissSetup) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        VaultSetupDialogContent(state = state, onAction = onAction)
    }
}

/** The web's "Enter Master Password" dialog, for a returning Google user. */
@Composable
fun VaultUnlockDialog(state: VaultUnlockDialogState, onAction: (GoogleAuthAction) -> Unit) {
    Dialog(
        onDismissRequest = { onAction(GoogleAuthAction.DismissUnlock) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        VaultUnlockDialogContent(state = state, onAction = onAction)
    }
}

@Composable
private fun VaultSetupDialogContent(state: VaultSetupDialogState, onAction: (GoogleAuthAction) -> Unit) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onAction(GoogleAuthAction.SubmitSetup)
    }
    VaultDialogCard(
        badge = stringResource(R.string.google_setup_badge),
        title = stringResource(R.string.google_setup_title),
        body = if (state.name.isBlank()) {
            stringResource(R.string.google_setup_body)
        } else {
            stringResource(R.string.google_setup_body_named, state.name)
        },
    ) {
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(GoogleAuthAction.SetupPasswordChanged(it)) },
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(GoogleAuthAction.ToggleSetupPasswordVisibility) },
            label = stringResource(R.string.google_setup_password_label),
            placeholder = stringResource(R.string.google_setup_password_placeholder),
            required = true,
            enabled = !state.isSubmitting,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            StrengthBars(password = state.password, strength = state.strength)
        }

        Spacer(Modifier.height(16.dp))
        XpPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(GoogleAuthAction.SetupConfirmPasswordChanged(it)) },
            isPasswordVisible = state.isConfirmPasswordVisible,
            onToggleVisibility = { onAction(GoogleAuthAction.ToggleSetupConfirmPasswordVisibility) },
            label = stringResource(R.string.google_setup_confirm_label),
            placeholder = stringResource(R.string.google_setup_confirm_placeholder),
            required = true,
            enabled = !state.isSubmitting,
            contentType = ContentType.NewPassword,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        DialogError(state.error)

        Spacer(Modifier.height(20.dp))
        XpActionButton(
            text = stringResource(R.string.google_setup_submit),
            loadingText = stringResource(R.string.google_setup_submitting),
            onClick = submit,
            isLoading = state.isSubmitting,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VaultUnlockDialogContent(state: VaultUnlockDialogState, onAction: (GoogleAuthAction) -> Unit) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val submit = {
        focusManager.clearFocus()
        onAction(GoogleAuthAction.SubmitUnlock)
    }
    VaultDialogCard(
        badge = stringResource(R.string.google_unlock_badge),
        title = stringResource(R.string.google_unlock_title),
        body = stringResource(R.string.google_unlock_body, state.email),
    ) {
        XpPasswordField(
            value = state.password,
            onValueChange = { onAction(GoogleAuthAction.UnlockPasswordChanged(it)) },
            isPasswordVisible = state.isPasswordVisible,
            onToggleVisibility = { onAction(GoogleAuthAction.ToggleUnlockPasswordVisibility) },
            placeholder = stringResource(R.string.google_unlock_placeholder),
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        // The web focuses the field when the dialog opens.
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

        DialogError(state.error)

        Spacer(Modifier.height(16.dp))
        XpActionButton(
            text = stringResource(R.string.google_unlock_submit),
            loadingText = stringResource(R.string.google_unlock_submitting),
            onClick = submit,
            isLoading = state.isSubmitting,
            enabled = state.password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.google_unlock_forgot),
            style = XpTheme.typography.body.copy(fontSize = 12.sp),
            color = XpTheme.colors.textLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) { onAction(GoogleAuthAction.ForgotMasterPassword) }
                .padding(vertical = 8.dp),
        )
    }
}

/** The dialogs' shared frame: key icon + small blue caption, title and explanation, then [content]. */
@Composable
private fun VaultDialogCard(
    badge: String,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.fieldBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_key),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = badge.uppercase(),
                style = typography.caption.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                color = colors.primary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(text = title, style = typography.dialogTitle, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        content()
    }
}

/** The web's meter: the first bar fills at 8 characters, the second from Medium, the third at Strong. */
@Composable
private fun StrengthBars(password: String, strength: MasterPasswordStrength) {
    val colors = XpTheme.colors
    val (color, label) = when (strength) {
        MasterPasswordStrength.WEAK -> colors.error to R.string.google_setup_strength_weak
        MasterPasswordStrength.MEDIUM -> colors.warning to R.string.google_setup_strength_medium
        MasterPasswordStrength.STRONG -> colors.success to R.string.google_setup_strength_strong
    }
    val filled = listOf(
        password.length >= 8,
        strength != MasterPasswordStrength.WEAK,
        strength == MasterPasswordStrength.STRONG,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.width(140.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            filled.forEach { isFilled ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isFilled) color else colors.fieldBorder),
                )
            }
        }
        Text(
            text = stringResource(label),
            style = XpTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun DialogError(error: UiText?) {
    if (error == null) return
    Spacer(Modifier.height(12.dp))
    Text(
        text = error.asString(),
        style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
        color = XpTheme.colors.error,
    )
}

@Preview(name = "Create master password", showBackground = true)
@Composable
private fun VaultSetupDialogPreview() {
    XproKeyTheme(darkTheme = false) {
        VaultSetupDialogContent(
            state = VaultSetupDialogState(name = "Vansh Goel", email = "goelv2610@gmail.com", password = "Vansh@2610", confirmPassword = "Vansh"),
            onAction = {},
        )
    }
}

@Preview(name = "Enter master password", showBackground = true)
@Composable
private fun VaultUnlockDialogPreview() {
    XproKeyTheme(darkTheme = true) {
        VaultUnlockDialogContent(
            state = VaultUnlockDialogState(
                email = "goelv2610@gmail.com",
                password = "secret",
                error = UiText.Resource(R.string.google_unlock_incorrect),
            ),
            onAction = {},
        )
    }
}
