package com.xprokeey2.presentation.onboarding.license

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents

@Composable
fun ActivateLicenseScreenRoot(
    onActivated: (organization: String?) -> Unit,
    onSessionExpired: (message: String) -> Unit,
    viewModel: ActivateLicenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ActivateLicenseEvent.Activated -> onActivated(event.organization)
            is ActivateLicenseEvent.SessionExpired -> onSessionExpired(event.message.asString(context))
        }
    }

    ActivateLicenseScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun ActivateLicenseScreen(
    state: ActivateLicenseUiState,
    onAction: (ActivateLicenseAction) -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val focusManager = LocalFocusManager.current
    val cardShape = RoundedCornerShape(20.dp)
    val monoStyle = typography.mono.copy(fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 2.sp)
    val submit = {
        focusManager.clearFocus()
        onAction(ActivateLicenseAction.Submit)
    }

    AuthScreenLayout(
        snackbarHostState = remember { SnackbarHostState() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        containerColor = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.divider, cardShape)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_check),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.license_title),
                style = typography.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.license_subtitle),
                style = typography.subtitle.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_key),
                    contentDescription = null,
                    tint = colors.textLabel,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.label_license_key).uppercase(),
                    style = typography.fieldLabel,
                    color = colors.textLabel,
                )
            }
            Spacer(Modifier.height(10.dp))
            XpTextField(
                value = state.licenseKey,
                onValueChange = { onAction(ActivateLicenseAction.LicenseKeyChanged(it)) },
                placeholder = stringResource(R.string.placeholder_license_key),
                error = state.licenseKeyError?.asString(),
                enabled = !state.isLoading,
                textStyle = monoStyle,
                placeholderStyle = monoStyle,
                // Keys are case-sensitive and may contain symbols: no auto-caps, no autocorrect.
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_key),
                        contentDescription = null,
                        tint = colors.textPlaceholder,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))
            XpPrimaryButton(
                text = stringResource(R.string.activate_license),
                onClick = submit,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.license_info),
                style = typography.body.copy(lineHeight = 18.5.sp),
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.fieldBackground)
                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                    .padding(18.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 900)
@Composable
private fun ActivateLicenseScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        ActivateLicenseScreen(state = ActivateLicenseUiState(), onAction = {})
    }
}

@Preview(name = "Dark", showBackground = true, heightDp = 900)
@Composable
private fun ActivateLicenseScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        ActivateLicenseScreen(state = ActivateLicenseUiState(licenseKey = "3TFbIEW%LGr-"), onAction = {})
    }
}
