package com.xprokeey2.presentation.passwords.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpTextField
import com.xprokeey2.presentation.passwords.components.CategoryChip
import com.xprokeey2.presentation.passwords.components.VaultItemAvatar
import com.xprokeey2.presentation.passwords.components.WebsiteLink
import com.xprokeey2.presentation.passwords.components.openLink
import com.xprokeey2.presentation.passwords.components.shortDate
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
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun PasswordsScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddPassword: () -> Unit,
    onViewItem: (itemId: Long) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: PasswordsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PasswordsEvent.ShowMessage -> scope.launch {
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is PasswordsEvent.CopyToClipboard -> {
                val label = event.label.asString(context)
                copyToClipboard(context, label = label, value = event.value, sensitive = false)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied, label)) }
            }
            is PasswordsEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    // e.g. "Password saved successfully." after coming back from Add password.
    ResultMessageEffect(resultMessage, snackbarHostState, onResultMessageShown)
    LifecycleResumeEffect(Unit) {
        viewModel.onAction(PasswordsAction.Refresh)
        onPauseOrDispose { }
    }

    PasswordsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onSectionClick = onSectionClick,
        onAddPassword = onAddPassword,
        onViewItem = onViewItem,
        onOpenLink = { url ->
            if (!openLink(uriHandler, url)) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.link_open_failed)) }
            }
        },
    )
}

@Composable
fun PasswordsScreen(
    state: PasswordsUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (PasswordsAction) -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onAddPassword: () -> Unit,
    onViewItem: (itemId: Long) -> Unit,
    onOpenLink: (url: String) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.PASSWORDS,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {
            item {
                // Like the web on phones: title and subtitle on their own lines, the button below
                // them at full width.
                Text(
                    text = stringResource(R.string.passwords_title),
                    style = XpTheme.typography.pageTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.passwords_subtitle),
                    style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                CompactButton(
                    text = stringResource(R.string.add_password_button),
                    icon = R.drawable.ic_plus,
                    onClick = onAddPassword,
                    style = CompactButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XpTextField(
                        value = state.query,
                        onValueChange = { onAction(PasswordsAction.QueryChanged(it)) },
                        placeholder = stringResource(R.string.search_passwords),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                tint = colors.textPlaceholder,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    FilterPicker(
                        selected = state.filter,
                        categories = state.categories,
                        onSelected = { onAction(PasswordsAction.FilterSelected(it)) },
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (state.selectedIds.isNotEmpty()) {
                    SelectionBar(
                        count = state.selectedIds.size,
                        onCancel = { onAction(PasswordsAction.ClearSelection) },
                        onDelete = { onAction(PasswordsAction.DeleteClicked) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            val visible = state.visibleItems
            when {
                state.isLoading -> item { LoadingBlock() }
                state.loadError != null -> item {
                    MessagePanel(
                        title = stringResource(R.string.passwords_load_failed),
                        body = state.loadError.asString(),
                        buttonText = stringResource(R.string.retry),
                        onButtonClick = { onAction(PasswordsAction.Refresh) },
                    )
                }
                visible.isEmpty() -> item {
                    MessagePanel(
                        title = stringResource(R.string.passwords_empty_title),
                        body = stringResource(R.string.passwords_empty_body),
                    )
                }
                else -> {
                    item {
                        TableHeader(
                            allSelected = visible.all { it.id in state.selectedIds },
                            onToggleAll = { onAction(PasswordsAction.ToggleSelectAll) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    items(visible, key = { item -> item.id }) { item ->
                        PasswordCard(
                            item = item,
                            isSelected = item.id in state.selectedIds,
                            isUpdatingFavorite = item.id in state.favoriteUpdatingIds,
                            onToggleSelected = { onAction(PasswordsAction.ToggleSelected(item.id)) },
                            onToggleFavorite = { onAction(PasswordsAction.ToggleFavorite(item.id)) },
                            onView = { onViewItem(item.id) },
                            onCopyUsername = { onAction(PasswordsAction.CopyUsername(item)) },
                            onOpenLink = onOpenLink,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }

    if (state.isDeleteDialogVisible) {
        val count = state.selectedIds.size
        ConfirmDeleteDialog(
            title = pluralStringResource(R.plurals.delete_passwords_title, count, count),
            body = pluralStringResource(R.plurals.delete_passwords_body, count),
            isDeleting = state.isDeleting,
            onConfirm = { onAction(PasswordsAction.DeleteConfirmed) },
            onDismiss = { onAction(PasswordsAction.DeleteDismissed) },
        )
    }
}

/** Web filter dropdown: All Items, Favorites, Weak Items, then the categories. */
@Composable
private fun FilterPicker(
    selected: PasswordFilter,
    categories: List<String>,
    onSelected: (PasswordFilter) -> Unit,
) {
    val colors = XpTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)
    val label = when (selected) {
        PasswordFilter.All -> stringResource(R.string.filter_all_items)
        PasswordFilter.Favorites -> stringResource(R.string.filter_favorites)
        PasswordFilter.Weak -> stringResource(R.string.filter_weak_items)
        is PasswordFilter.Category -> selected.name
    }

    Box {
        Row(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 150.dp)
                .height(48.dp)
                .clip(shape)
                .background(colors.fieldBackground)
                .border(1.dp, if (expanded) colors.primary else colors.fieldBorder, shape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = XpTheme.typography.fieldText.copy(fontSize = 13.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(15.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.surface,
        ) {
            val choose = { filter: PasswordFilter ->
                expanded = false
                onSelected(filter)
            }
            FilterMenuItem(stringResource(R.string.filter_all_items), selected == PasswordFilter.All) { choose(PasswordFilter.All) }
            FilterMenuItem(stringResource(R.string.filter_favorites), selected == PasswordFilter.Favorites) { choose(PasswordFilter.Favorites) }
            FilterMenuItem(
                text = stringResource(R.string.filter_weak_items),
                isSelected = selected == PasswordFilter.Weak,
                color = colors.warning,
            ) { choose(PasswordFilter.Weak) }
            if (categories.isNotEmpty()) HorizontalDivider(color = colors.divider)
            categories.forEach { category ->
                FilterMenuItem(category, selected == PasswordFilter.Category(category)) {
                    choose(PasswordFilter.Category(category))
                }
            }
        }
    }
}

@Composable
private fun FilterMenuItem(text: String, isSelected: Boolean, color: Color? = null, onClick: () -> Unit) {
    val colors = XpTheme.colors
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = XpTheme.typography.fieldText.copy(
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = color ?: colors.textPrimary,
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
        onClick = onClick,
    )
}

/** "1 selected   Cancel   Delete (1)", shown while items are ticked. */
@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onDelete: () -> Unit) {
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.selected_count, count),
                style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp),
                color = XpTheme.colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            )
            CompactButton(text = stringResource(R.string.cancel), icon = null, onClick = onCancel)
            Spacer(Modifier.width(8.dp))
            CompactButton(
                text = stringResource(R.string.delete_selected, count),
                icon = R.drawable.ic_trash,
                onClick = onDelete,
                style = CompactButtonStyle.Danger,
            )
        }
    }
}

@Composable
private fun TableHeader(allSelected: Boolean, onToggleAll: () -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectAll = stringResource(R.string.cd_select_all)
        SelectBox(
            checked = allSelected,
            onToggle = onToggleAll,
            modifier = Modifier.semantics { contentDescription = selectAll },
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.column_item).uppercase(),
            style = XpTheme.typography.fieldLabel,
            color = colors.textLabel,
        )
    }
}

/**
 * One password as a card. Portrait: checkbox, logo, name, website and username with the star in the
 * corner; below them a full-width row with the category, date, view and copy (like the web's phone
 * card, so nothing is squeezed next to the logo). Landscape: one row with the buttons on the right.
 * The checkbox keeps selecting several passwords working.
 */
@Composable
private fun PasswordCard(
    item: VaultItem,
    isSelected: Boolean,
    isUpdatingFavorite: Boolean,
    onToggleSelected: () -> Unit,
    onToggleFavorite: () -> Unit,
    onView: () -> Unit,
    onCopyUsername: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val selectLabel = stringResource(R.string.cd_select_item, item.title)
    val cardModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(if (isSelected) colors.primary.copy(alpha = 0.06f) else colors.surface)
        .border(1.dp, colors.divider, shape)
        .clickable(onClick = onView)

    if (isLandscape) {
        Row(modifier = cardModifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectBox(
                checked = isSelected,
                onToggle = onToggleSelected,
                modifier = Modifier.semantics { contentDescription = selectLabel },
            )
            Spacer(Modifier.width(10.dp))
            VaultItemAvatar(title = item.title, url = item.url, size = 40.dp, softLetter = true)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardTitle(item.title, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    CategoryChip(category = item.category, modifier = Modifier.widthIn(max = 130.dp))
                }
                CardUsername(item.username)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CardWebsite(
                        url = item.url,
                        onOpenLink = onOpenLink,
                        showIcon = false,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val date = item.updatedAt.shortDate()
                    if (date.isNotEmpty()) CardDate(stringResource(R.string.dot_separator) + date)
                }
            }
            FavoriteButton(isFavorite = item.isFavorite, enabled = !isUpdatingFavorite, onClick = onToggleFavorite)
            ViewButton(onClick = onView)
            CopyButton(enabled = item.username.isNotBlank(), onClick = onCopyUsername)
        }
        return
    }

    Column(modifier = cardModifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            SelectBox(
                checked = isSelected,
                onToggle = onToggleSelected,
                modifier = Modifier
                    // Level with the middle of the logo.
                    .padding(top = 11.dp)
                    .semantics { contentDescription = selectLabel },
            )
            Spacer(Modifier.width(12.dp))
            VaultItemAvatar(title = item.title, url = item.url, size = 40.dp, softLetter = true)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                CardTitle(item.title)
                CardWebsite(url = item.url, onOpenLink = onOpenLink, showIcon = true)
                CardUsername(item.username)
            }
            // In the top corner, level with the name.
            FavoriteButton(
                isFavorite = item.isFavorite,
                enabled = !isUpdatingFavorite,
                onClick = onToggleFavorite,
                modifier = Modifier.offset(x = 6.dp, y = (-12).dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryChip(category = item.category, modifier = Modifier.widthIn(max = 140.dp))
            Spacer(Modifier.width(10.dp))
            CardDate(item.updatedAt.shortDate(), modifier = Modifier.weight(1f))
            ViewButton(onClick = onView)
            Spacer(Modifier.width(4.dp))
            CopyButton(enabled = item.username.isNotBlank(), onClick = onCopyUsername)
        }
    }
}

@Composable
private fun CardTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = XpTheme.typography.bodyBold.copy(fontSize = 15.sp, lineHeight = 20.sp),
        color = XpTheme.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun CardUsername(username: String) {
    Text(
        text = username.ifBlank { stringResource(R.string.empty_value) },
        style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = XpTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The website link, or "No URL". */
@Composable
private fun CardWebsite(url: String, onOpenLink: (String) -> Unit, showIcon: Boolean, modifier: Modifier = Modifier) {
    if (url.isNotBlank()) {
        WebsiteLink(url = url, onOpen = onOpenLink, showIcon = showIcon, modifier = modifier)
    } else {
        Text(
            text = stringResource(R.string.no_url),
            style = XpTheme.typography.body.copy(fontSize = 12.sp),
            color = XpTheme.colors.textLabel,
            maxLines = 1,
            modifier = modifier,
        )
    }
}

@Composable
private fun CardDate(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = XpTheme.typography.body.copy(fontSize = 12.sp),
        color = XpTheme.colors.textLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Tap to add to / remove from Favorites. */
@Composable
private fun FavoriteButton(isFavorite: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            painter = painterResource(if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star),
            contentDescription = stringResource(if (isFavorite) R.string.cd_remove_favorite else R.string.cd_add_favorite),
            tint = if (isFavorite) Color(0xFFF5B301) else XpTheme.colors.textLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The eye: opens the password's details. */
@Composable
private fun ViewButton(onClick: () -> Unit) {
    val label = stringResource(R.string.cd_view_details)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_eye),
            contentDescription = null,
            tint = XpTheme.colors.textSecondary,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** Copies the username, on a light blue square. */
@Composable
private fun CopyButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val label = stringResource(R.string.cd_copy_username)
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 36.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.1f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_copy),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(17.dp),
        )
    }
}

/** Plain 18dp checkbox for list selection. */
@Composable
private fun SelectBox(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(shape)
            .background(if (checked) colors.primary else colors.fieldBackground)
            .border(1.dp, if (checked) colors.primary else colors.checkboxBorder, shape)
            .clickable(role = Role.Checkbox, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun MessagePanel(
    title: String,
    body: String,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 28.dp) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconTile(icon = R.drawable.ic_search, tint = colors.primary, size = 46.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = XpTheme.typography.sectionTitle,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (buttonText != null) {
                Spacer(Modifier.height(18.dp))
                XpPrimaryButton(text = buttonText, onClick = onButtonClick)
            }
        }
    }
}

private val PreviewItems = listOf(
    VaultItem(1, "Github", "Vanshgoel2610", "https://github.com", "", "Personal", true, null, Instant.parse("2026-09-28T10:15:00Z")),
    VaultItem(2, "AppLock", "Vansh", "https://applock.com", "", "Travel", false, null, Instant.parse("2026-09-28T10:18:00Z")),
    VaultItem(3, "Vansh", "vansh123", "", "", "Others", false, null, Instant.parse("2026-09-28T11:02:00Z")),
    VaultItem(4, "Krishna", "krishna123", "", "", "Others", false, null, Instant.parse("2026-09-28T11:04:00Z")),
)

@Preview(name = "List", showBackground = true, heightDp = 1100)
@Composable
private fun PasswordsScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        PasswordsScreen(
            state = PasswordsUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                isLoading = false,
                items = PreviewItems,
                categories = listOf("Personal", "Work", "Travel", "Others"),
                weakItemIds = setOf(2, 3, 4),
                selectedIds = setOf(2),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onSectionClick = {}, onAddPassword = {}, onViewItem = {}, onOpenLink = {},
        )
    }
}

@Preview(name = "Landscape", showBackground = true, device = "spec:width=891dp,height=411dp")
@Composable
private fun PasswordsScreenLandscapePreview() {
    XproKeyTheme(darkTheme = false) {
        PasswordsScreen(
            state = PasswordsUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                isLoading = false,
                items = PreviewItems,
                categories = listOf("Personal", "Work", "Travel", "Others"),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onSectionClick = {}, onAddPassword = {}, onViewItem = {}, onOpenLink = {},
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun PasswordsScreenEmptyPreview() {
    XproKeyTheme(darkTheme = false) {
        PasswordsScreen(
            state = PasswordsUiState(isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}, onSectionClick = {}, onAddPassword = {}, onViewItem = {}, onOpenLink = {},
        )
    }
}
