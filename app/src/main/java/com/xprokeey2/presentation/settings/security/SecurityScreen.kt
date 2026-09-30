package com.xprokeey2.presentation.settings.security

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.presentation.components.XpActionButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun SecurityScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onCancel: () -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SecurityEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is SecurityEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    SecurityScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onCancel = onCancel,
        onSectionClick = onSectionClick,
    )
}

/** Settings > Security, laid out like the web page. */
@Composable
fun SecurityScreen(
    state: SecurityUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (SecurityAction) -> Unit,
    onCancel: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.SECURITY,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.security_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.security_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                if (state.isLoading) {
                    LoadingBlock()
                } else {
                    SecurityForm(state = state, onAction = onAction, onCancel = onCancel)
                }
            }
        }
    }
}

@Composable
private fun SecurityForm(state: SecurityUiState, onAction: (SecurityAction) -> Unit, onCancel: () -> Unit) {
    val colors = XpTheme.colors
    SectionLabel(icon = R.drawable.ic_clock, text = stringResource(R.string.security_timeout_after))
    Spacer(Modifier.height(8.dp))
    DurationPicker(
        selected = state.duration,
        onSelected = { onAction(SecurityAction.DurationSelected(it)) },
    )

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = colors.divider)
    Spacer(Modifier.height(24.dp))

    SectionLabel(icon = R.drawable.ic_shield_check, text = stringResource(R.string.security_when_timeout))
    Spacer(Modifier.height(12.dp))
    ActionOptions(selected = state.action, onSelected = { onAction(SecurityAction.ActionSelected(it)) })

    Spacer(Modifier.height(24.dp))
    InfoBanner()

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = colors.divider)
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    ) {
        XpSecondaryButton(
            text = stringResource(R.string.cancel),
            onClick = onCancel,
            modifier = Modifier.height(44.dp),
        )
        XpActionButton(
            text = stringResource(if (state.isSaved) R.string.security_saved else R.string.security_save),
            loadingText = stringResource(R.string.security_saving),
            isLoading = state.isSaving,
            icon = if (state.isSaved) null else R.drawable.ic_save,
            onClick = { onAction(SecurityAction.Save) },
            modifier = Modifier.widthIn(min = 140.dp),
        )
    }
}

/** "TIMEOUT AFTER" / "WHEN TIMEOUT IS REACHED" with its icon. */
@Composable
private fun SectionLabel(@DrawableRes icon: Int, text: String) {
    val colors = XpTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.textLabel,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text.uppercase(),
            style = XpTheme.typography.fieldLabel,
            color = colors.textLabel,
        )
    }
}

/** The web's select: the current value with a chevron, the options in a menu. */
@Composable
private fun DurationPicker(selected: TimeoutDuration, onSelected: (TimeoutDuration) -> Unit) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    Box {
        Row(
            modifier = Modifier
                .height(44.dp)
                .widthIn(min = 130.dp)
                .clip(shape)
                .background(colors.fieldBackground)
                .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(selected.label),
                style = XpTheme.typography.fieldText,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = colors.surface) {
            TimeoutDuration.entries.forEach { duration ->
                val isSelected = duration == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(duration.label),
                            style = XpTheme.typography.fieldText.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = colors.textPrimary,
                        )
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(duration)
                    },
                )
            }
        }
    }
}

/** Log out / Lock: side by side when there's room, like the web's two-column grid. */
@Composable
private fun ActionOptions(selected: TimeoutAction, onSelected: (TimeoutAction) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 520.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TimeoutAction.entries.forEach { action ->
                    ActionCard(action, action == selected, { onSelected(action) }, Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TimeoutAction.entries.forEach { action ->
                    ActionCard(action, action == selected, { onSelected(action) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun ActionCard(action: TimeoutAction, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primary.copy(alpha = if (colors.isDark) 0.10f else 0.05f) else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.divider, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) colors.primary.copy(alpha = 0.14f) else colors.fieldBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.textLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (action == TimeoutAction.LOGOUT) {
                val badgeShape = RoundedCornerShape(6.dp)
                Text(
                    text = stringResource(R.string.security_recommended).uppercase(),
                    style = XpTheme.typography.caption.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                    color = colors.success,
                    modifier = Modifier
                        .clip(badgeShape)
                        .background(colors.success.copy(alpha = if (colors.isDark) 0.15f else 0.08f))
                        .border(1.dp, colors.success.copy(alpha = 0.25f), badgeShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(action.title),
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
            color = if (selected) colors.primary else colors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(action.description),
            style = XpTheme.typography.body.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
            color = colors.textSecondary,
        )
    }
}

/** "Log out is the more secure option … Lock keeps keys …", with both option names in bold. */
@Composable
private fun InfoBanner() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val logOut = stringResource(R.string.security_action_logout)
    val lock = stringResource(R.string.security_action_lock)
    val text = stringResource(R.string.security_info, logOut, lock)
    val annotated = buildAnnotatedString {
        append(text)
        val logOutAt = text.indexOf(logOut)
        if (logOutAt >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), logOutAt, logOutAt + logOut.length)
        val lockAt = text.indexOf(lock, startIndex = (logOutAt + logOut.length).coerceAtLeast(0))
        if (lockAt >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), lockAt, lockAt + lock.length)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.08f else 0.05f))
            .border(1.dp, colors.primary.copy(alpha = if (colors.isDark) 0.2f else 0.12f), shape)
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = annotated,
            style = XpTheme.typography.body.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
            color = colors.primary,
        )
    }
}

@get:StringRes
private val TimeoutDuration.label: Int
    get() = when (this) {
        TimeoutDuration.ONE_MINUTE -> R.string.security_timeout_1_minute
        TimeoutDuration.FIVE_MINUTES -> R.string.security_timeout_5_minutes
        TimeoutDuration.FIFTEEN_MINUTES -> R.string.security_timeout_15_minutes
        TimeoutDuration.THIRTY_MINUTES -> R.string.security_timeout_30_minutes
        TimeoutDuration.ONE_HOUR -> R.string.security_timeout_1_hour
        TimeoutDuration.FOUR_HOURS -> R.string.security_timeout_4_hours
        TimeoutDuration.NEVER -> R.string.security_timeout_never
    }

@get:DrawableRes
private val TimeoutAction.icon: Int
    get() = when (this) {
        TimeoutAction.LOGOUT -> R.drawable.ic_log_out
        TimeoutAction.LOCK -> R.drawable.ic_lock
    }

@get:StringRes
private val TimeoutAction.title: Int
    get() = when (this) {
        TimeoutAction.LOGOUT -> R.string.security_action_logout
        TimeoutAction.LOCK -> R.string.security_action_lock
    }

@get:StringRes
private val TimeoutAction.description: Int
    get() = when (this) {
        TimeoutAction.LOGOUT -> R.string.security_action_logout_description
        TimeoutAction.LOCK -> R.string.security_action_lock_description
    }

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SecurityScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        SecurityScreen(
            state = SecurityUiState(
                user = UserBadge.from(name = "Vansh Goel", email = "goelv2610@gmail.com"),
                isLoading = false,
                action = TimeoutAction.LOCK,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onCancel = {},
            onSectionClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, widthDp = 720)
@Composable
private fun SecurityScreenWidePreview() {
    XproKeyTheme(darkTheme = false) {
        SecurityScreen(
            state = SecurityUiState(isLoading = false, duration = TimeoutDuration.FIFTEEN_MINUTES, isSaved = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onCancel = {},
            onSectionClick = {},
        )
    }
}
