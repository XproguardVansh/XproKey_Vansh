package com.xprokeey2.presentation.passwords.details

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultItemDetails
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.passwords.components.VaultItemAvatar
import com.xprokeey2.presentation.passwords.components.WebsiteLink
import com.xprokeey2.presentation.passwords.components.dateTime
import com.xprokeey2.presentation.passwords.components.displayDomain
import com.xprokeey2.presentation.passwords.components.openLink
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.ResultMessageEffect
import com.xprokeey2.presentation.util.copyToClipboard
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.ConfirmDeleteDialog
import com.xprokeey2.presentation.workspace.IconTile
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun PasswordDetailsScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onEdit: () -> Unit,
    onDeleted: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: PasswordDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PasswordDetailsEvent.CopyToClipboard -> {
                val label = event.label.asString(context)
                copyToClipboard(context, label = label, value = event.value, sensitive = event.sensitive)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied, label)) }
            }
            is PasswordDetailsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is PasswordDetailsEvent.Deleted -> onDeleted(event.message.asString(context))
            is PasswordDetailsEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    // e.g. "Password updated successfully." after coming back from Edit.
    ResultMessageEffect(resultMessage, snackbarHostState, onResultMessageShown)
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(PasswordDetailsAction.Refresh)
        onPauseOrDispose { }
    }

    PasswordDetailsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onSectionClick = onSectionClick,
        onEdit = onEdit,
        onOpenLink = { url ->
            if (!openLink(uriHandler, url)) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_open_failed)) }
            }
        },
    )
}

@Composable
fun PasswordDetailsScreen(
    state: PasswordDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (PasswordDetailsAction) -> Unit,
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onEdit: () -> Unit,
    onOpenLink: (url: String) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.PASSWORDS,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            val details = state.details
            when {
                state.isLoading -> LoadingBlock()
                details == null -> WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
                    Text(
                        text = state.loadError?.asString().orEmpty(),
                        style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    XpPrimaryButton(
                        text = stringResource(R.string.retry),
                        onClick = { onAction(PasswordDetailsAction.Refresh) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> DetailsContent(
                    details = details,
                    isPasswordVisible = state.isPasswordVisible,
                    onAction = onAction,
                    onEdit = onEdit,
                    onOpenLink = onOpenLink,
                )
            }
        }
    }

    val item = state.details?.item
    if (state.isDeleteDialogVisible && item != null) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_password_title),
            body = stringResource(R.string.delete_password_body, item.title),
            isDeleting = state.isDeleting,
            onConfirm = { onAction(PasswordDetailsAction.DeleteConfirmed) },
            onDismiss = { onAction(PasswordDetailsAction.DeleteDismissed) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(
    details: VaultItemDetails,
    isPasswordVisible: Boolean,
    onAction: (PasswordDetailsAction) -> Unit,
    onEdit: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val colors = XpTheme.colors
    val item = details.item
    val copy = { field: VaultField -> onAction(PasswordDetailsAction.Copy(field)) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = colors.textLabel,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.vaults_caption),
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = colors.textSecondary,
        )
    }
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        VaultItemAvatar(title = item.title, url = item.url, size = 56.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.title,
            style = XpTheme.typography.pageTitle,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(14.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item.updatedAt?.let { MetaChip(R.drawable.ic_clock, it.dateTime()) }
        if (item.category.isNotBlank()) MetaChip(R.drawable.ic_tag, item.category)
        MetaChip(
            icon = if (item.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star,
            text = stringResource(if (item.isFavorite) R.string.favorite else R.string.not_favorite),
            iconTint = if (item.isFavorite) Color(0xFFF5B301) else null,
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CompactButton(
            text = stringResource(R.string.edit),
            icon = R.drawable.ic_pencil,
            onClick = onEdit,
            modifier = Modifier.weight(1f),
        )
        CompactButton(
            text = stringResource(R.string.delete),
            icon = R.drawable.ic_trash,
            onClick = { onAction(PasswordDetailsAction.DeleteClicked) },
            style = CompactButtonStyle.Danger,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(24.dp))
    WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(R.drawable.ic_lock, stringResource(R.string.credentials))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CredentialRow(
                icon = R.drawable.ic_user,
                label = stringResource(R.string.label_username_email),
                onCopy = { copy(VaultField.USERNAME) }.takeIf { item.username.isNotBlank() },
            ) {
                ValueText(item.username.ifBlank { stringResource(R.string.empty_value) })
            }
            CredentialRow(
                icon = R.drawable.ic_globe,
                label = stringResource(R.string.label_website),
                onCopy = { copy(VaultField.WEBSITE) }.takeIf { item.url.isNotBlank() },
            ) {
                if (item.url.isBlank()) {
                    ValueText(stringResource(R.string.no_url))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ValueText(
                            text = displayDomain(item.url),
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        WebsiteLink(url = item.url, onOpen = onOpenLink, label = stringResource(R.string.visit))
                    }
                }
            }
            CredentialRow(
                icon = R.drawable.ic_lock,
                label = stringResource(R.string.label_password),
                onCopy = { copy(VaultField.PASSWORD) },
                trailing = {
                    IconButton(onClick = { onAction(PasswordDetailsAction.TogglePassword) }) {
                        Icon(
                            painter = painterResource(if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                            contentDescription = stringResource(
                                if (isPasswordVisible) R.string.cd_hide_password else R.string.cd_show_password
                            ),
                            tint = colors.textLabel,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                },
            ) {
                ValueText(
                    text = details.password?.takeIf { isPasswordVisible } ?: "••••••••",
                    mono = true,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        SectionTitle(R.drawable.ic_file_text, stringResource(R.string.details_section))
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (item.notes.isNotBlank()) {
                CredentialRow(
                    icon = R.drawable.ic_file_text,
                    label = stringResource(R.string.label_notes),
                    onCopy = { copy(VaultField.NOTES) },
                ) {
                    ValueText(item.notes, singleLine = false)
                }
            }
            CredentialRow(icon = R.drawable.ic_clock, label = stringResource(R.string.timestamps), onCopy = null) {
                TimestampLine(stringResource(R.string.created_label), item.createdAt.dateTime())
                TimestampLine(stringResource(R.string.updated_label), item.updatedAt.dateTime())
            }
        }
    }
}

@Composable
private fun MetaChip(@DrawableRes icon: Int, text: String, iconTint: Color? = null) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, colors.divider, shape)
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint ?: colors.textLabel,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = XpTheme.typography.body.copy(fontSize = 11.5.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun SectionTitle(@DrawableRes icon: Int, text: String) {
    val colors = XpTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = colors.textLabel, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text.uppercase(), style = XpTheme.typography.fieldLabel.copy(fontSize = 11.sp), color = colors.textLabel)
    }
}

/** Web "Credentials" row: icon tile, label + value, then extra buttons and "Copy". */
@Composable
private fun CredentialRow(
    @DrawableRes icon: Int,
    label: String,
    onCopy: (() -> Unit)?,
    trailing: (@Composable () -> Unit)? = null,
    value: @Composable () -> Unit,
) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.fieldBackground)
            .border(1.dp, colors.divider, shape)
            .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, tint = colors.textSecondary, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label.uppercase(), style = XpTheme.typography.fieldLabel, color = colors.textLabel)
            Spacer(Modifier.height(4.dp))
            value()
        }
        trailing?.invoke()
        if (onCopy != null) {
            CompactButton(text = stringResource(R.string.copy), icon = R.drawable.ic_copy, onClick = onCopy)
        }
    }
}

@Composable
private fun ValueText(text: String, modifier: Modifier = Modifier, mono: Boolean = false, singleLine: Boolean = true) {
    Text(
        text = text,
        style = if (mono) {
            XpTheme.typography.mono.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.5.sp)
        } else {
            XpTheme.typography.bodyBold.copy(fontSize = 13.5.sp, lineHeight = 19.sp)
        },
        color = XpTheme.colors.textPrimary,
        maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun TimestampLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = XpTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = XpTheme.colors.textPrimary,
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun PasswordDetailsScreenPreview() {
    val at = Instant.parse("2026-09-28T10:18:00Z")
    XproKeyTheme(darkTheme = true) {
        PasswordDetailsScreen(
            state = PasswordDetailsUiState(
                isLoading = false,
                details = VaultItemDetails(
                    item = VaultItem(2, "AppLock", "Vansh", "https://applock.com", "", "Travel", false, at, at),
                    password = "walldfs",
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onBack = {}, onSectionClick = {}, onEdit = {}, onOpenLink = {},
        )
    }
}
