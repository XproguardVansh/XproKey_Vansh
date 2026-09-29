package com.xprokeey2.presentation.dashboard

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.VaultItem
import com.xprokeey2.domain.model.VaultSecurity
import com.xprokeey2.presentation.passwords.components.CategoryChip
import com.xprokeey2.presentation.passwords.components.RecentItemIcon
import com.xprokeey2.presentation.passwords.form.DEFAULT_CATEGORY
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.CompactButton
import com.xprokeey2.presentation.workspace.CompactButtonStyle
import com.xprokeey2.presentation.workspace.IconTile
import com.xprokeey2.presentation.workspace.PageHeader
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddPassword: () -> Unit,
    onOpenWeakItems: () -> Unit,
    onViewPassword: (itemId: Long) -> Unit,
    onManageCards: () -> Unit,
    onSessionExpired: (message: String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is DashboardEvent.SessionExpired -> onSessionExpired(event.message.asString(context))
        }
    }
    // Also runs on first display, and again when coming back from Cards or Passwords.
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(DashboardAction.Refresh)
        onPauseOrDispose { }
    }

    DashboardScreen(
        state = state,
        onSectionClick = onSectionClick,
        onAddPassword = onAddPassword,
        onOpenWeakItems = onOpenWeakItems,
        onViewPassword = onViewPassword,
        onManageCards = onManageCards,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddPassword: () -> Unit,
    onOpenWeakItems: () -> Unit,
    onViewPassword: (itemId: Long) -> Unit,
    onManageCards: () -> Unit,
) {
    val colors = XpTheme.colors
    val security = state.security
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.DASHBOARD,
        onSectionClick = onSectionClick,
        snackbarHostState = remember { SnackbarHostState() },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            PageHeader(
                title = stringResource(R.string.dashboard_title),
                action = {
                    CompactButton(
                        text = stringResource(R.string.add_password),
                        icon = R.drawable.ic_plus,
                        onClick = onAddPassword,
                        style = CompactButtonStyle.Primary,
                    )
                },
            )

            Spacer(Modifier.height(20.dp))
            val unknown = stringResource(R.string.stat_unknown)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    title = stringResource(R.string.stat_passwords),
                    value = state.passwordCount?.toString() ?: unknown,
                    caption = stringResource(R.string.stat_passwords_caption),
                    captionColor = colors.success,
                    icon = R.drawable.ic_key,
                    iconTint = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    title = stringResource(R.string.stat_cards),
                    value = state.cardCount?.toString() ?: unknown,
                    caption = stringResource(R.string.stat_cards_caption),
                    captionColor = colors.warning,
                    icon = R.drawable.ic_credit_card,
                    iconTint = colors.primary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val (scoreCaption, scoreColor) = when (security?.rating) {
                    VaultSecurity.Rating.EXCELLENT -> stringResource(R.string.security_excellent) to colors.success
                    VaultSecurity.Rating.GOOD -> stringResource(R.string.security_good) to colors.warning
                    VaultSecurity.Rating.NEEDS_REVIEW -> stringResource(R.string.security_needs_review) to colors.warning
                    VaultSecurity.Rating.NO_ITEMS, null -> stringResource(R.string.stat_security_caption) to colors.textLabel
                }
                StatTile(
                    title = stringResource(R.string.stat_security_score),
                    value = security?.takeIf { it.hasItems }?.let { stringResource(R.string.security_percent, it.score) } ?: unknown,
                    caption = scoreCaption,
                    captionColor = scoreColor,
                    icon = R.drawable.ic_shield,
                    iconTint = colors.primary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    title = stringResource(R.string.stat_weak_items),
                    value = security?.weakCount?.toString() ?: unknown,
                    caption = stringResource(R.string.stat_weak_caption),
                    captionColor = colors.warning,
                    icon = R.drawable.ic_alert_triangle,
                    iconTint = colors.warning,
                    onClick = onOpenWeakItems,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.recently_updated),
                    style = XpTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.view_all),
                    style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { onSectionClick(WorkspaceSection.PASSWORDS) }
                        .padding(4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            RecentlyUpdated(items = state.recentItems, onViewPassword = onViewPassword)

            Spacer(Modifier.height(28.dp))
            WorkspacePanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.quick_actions),
                    style = XpTheme.typography.sectionTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Tools (generator, import) aren't built yet.
                    QuickAction(R.drawable.ic_wand, stringResource(R.string.action_generate_password), onClick = null)
                    QuickAction(R.drawable.ic_download, stringResource(R.string.action_import_passwords), onClick = null)
                    QuickAction(R.drawable.ic_credit_card, stringResource(R.string.action_manage_cards), onClick = onManageCards)
                }
                Spacer(Modifier.height(14.dp))
                SecurityBox(security)
            }
        }
    }
}

@Composable
private fun RecentlyUpdated(items: List<VaultItem>?, onViewPassword: (Long) -> Unit) {
    val colors = XpTheme.colors
    if (items.isNullOrEmpty()) {
        WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
            Text(
                text = stringResource(
                    if (items == null) R.string.recently_updated_placeholder else R.string.recently_updated_empty
                ),
                style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = colors.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { onViewPassword(item.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecentItemIcon(title = item.title, url = item.url)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = XpTheme.typography.bodyBold.copy(fontSize = 13.5.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.username.ifEmpty { stringResource(R.string.no_username) },
                        style = XpTheme.typography.body.copy(fontSize = 11.5.sp),
                        color = colors.textLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    CategoryChip(category = item.category.ifEmpty { DEFAULT_CATEGORY })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.updatedAt?.atZone(ZoneId.systemDefault())?.format(RecentDate)
                            ?: stringResource(R.string.just_now),
                        style = XpTheme.typography.body.copy(fontSize = 11.sp),
                        color = colors.textLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    caption: String,
    captionColor: Color,
    @DrawableRes icon: Int,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = XpTheme.colors
    WorkspacePanel(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onClick)
            } else {
                Modifier
            }
        ),
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = title.uppercase(),
                style = XpTheme.typography.fieldLabel.copy(fontSize = 10.5.sp),
                color = colors.textLabel,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp),
            )
            IconTile(icon = icon, tint = iconTint, size = 32.dp)
        }
        Text(
            text = value,
            style = XpTheme.typography.statValue,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = caption,
            style = XpTheme.typography.bodyBold.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = captionColor,
        )
    }
}

/** A quick action row; without [onClick] it is shown but not available yet. */
@Composable
private fun QuickAction(@DrawableRes icon: Int, title: String, onClick: (() -> Unit)?) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.fieldBackground)
            .border(1.dp, colors.divider, shape)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .alpha(if (onClick != null) 1f else 0.5f)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon = icon, tint = colors.primary, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp),
            color = colors.textPrimary,
        )
    }
}

/** Blue "Security" card: "80% protected" and a bar filled to the score. */
@Composable
private fun SecurityBox(security: VaultSecurity?) {
    val shape = RoundedCornerShape(14.dp)
    val score = security?.takeIf { it.hasItems }?.score
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF3B6FF0), Color(0xFF2549D6))))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.security_caption).uppercase(),
            style = XpTheme.typography.mono.copy(fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 1.3.sp),
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(8.dp))
        if (score != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.security_percent, score),
                    style = XpTheme.typography.statValue.copy(fontSize = 22.sp, lineHeight = 26.sp),
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.security_protected),
                    style = XpTheme.typography.body.copy(fontSize = 11.5.sp),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.security_placeholder),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
                color = Color.White,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            if (score != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(score / 100f)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White),
                )
            }
        }
    }
}

/** Web: `toLocaleDateString("en-US", { month: "short", day: "2-digit", year: "numeric" })`. */
private val RecentDate = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

private val PreviewItems = listOf(
    VaultItem(2, "AppLock", "Vansh", "https://applock.com", "", "Travel", false, null, Instant.parse("2026-09-28T10:18:00Z")),
    VaultItem(1, "Github", "Vanshgoel2610", "https://github.com", "", "Personal", true, null, Instant.parse("2026-09-28T10:15:00Z")),
)

@Preview(name = "Light", showBackground = true, heightDp = 1200)
@Composable
private fun DashboardScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        DashboardScreen(
            state = DashboardUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                cardCount = 1,
                passwordCount = 2,
                security = VaultSecurity.calculate(PreviewItems, missingFieldsCount = 0, markedWeakIds = setOf(2)),
                recentItems = PreviewItems,
            ),
            onSectionClick = {}, onAddPassword = {}, onOpenWeakItems = {}, onViewPassword = {}, onManageCards = {},
        )
    }
}

@Preview(name = "Dark, empty", showBackground = true, heightDp = 1200)
@Composable
private fun DashboardScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                passwordCount = 0,
                security = VaultSecurity.calculate(emptyList(), missingFieldsCount = 0, markedWeakIds = emptySet()),
                recentItems = emptyList(),
            ),
            onSectionClick = {}, onAddPassword = {}, onOpenWeakItems = {}, onViewPassword = {}, onManageCards = {},
        )
    }
}
