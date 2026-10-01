package com.xprokeey2.presentation.workspace

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.session.LocalLogOut
import com.xprokeey2.presentation.session.SessionTimeoutEffect
import com.xprokeey2.presentation.theme.LocalThemeToggle
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import kotlinx.coroutines.launch

/** A sub-menu of the drawer; the entry whose [WorkspaceSection.opens] it is shows or hides it. */
enum class MenuGroup { TOOLS, SETTINGS, ABOUT }

/**
 * Sidebar sections of the web app. Only the ones in [WorkspaceSection.isAvailable] work so far.
 * [group] is the sub-menu an entry sits in (null = top level); an entry with [opens] opens that
 * sub-menu instead of a screen, like Tools, Settings and About on the web. [inDrawer] is false for
 * pages opened elsewhere, such as Profile from the account menu.
 */
enum class WorkspaceSection(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val isAvailable: Boolean,
    val group: MenuGroup? = null,
    val opens: MenuGroup? = null,
    val inDrawer: Boolean = true,
) {
    DASHBOARD(R.string.nav_dashboard, R.drawable.ic_layout_grid, isAvailable = true),
    PASSWORDS(R.string.nav_passwords, R.drawable.ic_lock, isAvailable = true),
    CARDS(R.string.nav_cards, R.drawable.ic_credit_card, isAvailable = true),
    TOOLS(R.string.nav_tools, R.drawable.ic_wrench, isAvailable = true, opens = MenuGroup.TOOLS),
    GENERATOR(R.string.nav_generator, R.drawable.ic_wand, isAvailable = true, group = MenuGroup.TOOLS),
    EXPORT(R.string.nav_export, R.drawable.ic_download, isAvailable = true, group = MenuGroup.TOOLS),
    IMPORT(R.string.nav_import, R.drawable.ic_upload, isAvailable = true, group = MenuGroup.TOOLS),
    SETTINGS(R.string.nav_settings, R.drawable.ic_settings, isAvailable = true, opens = MenuGroup.SETTINGS),
    CHANGE_PASSWORD(R.string.nav_change_password, R.drawable.ic_key, isAvailable = true, group = MenuGroup.SETTINGS),
    SECURITY(R.string.nav_security, R.drawable.ic_shield_check, isAvailable = true, group = MenuGroup.SETTINGS),
    SUBSCRIPTION(R.string.nav_subscription, R.drawable.ic_shield, isAvailable = true, group = MenuGroup.SETTINGS),
    ABOUT(R.string.nav_about, R.drawable.ic_info, isAvailable = true, group = MenuGroup.SETTINGS, opens = MenuGroup.ABOUT),
    APP_INFO(R.string.nav_app_info, R.drawable.ic_info, isAvailable = true, group = MenuGroup.ABOUT),
    FAQ(R.string.nav_faq, R.drawable.ic_file_text, isAvailable = true, group = MenuGroup.ABOUT),
    SUPPORT(R.string.nav_support, R.drawable.ic_headphones, isAvailable = true),
    PROFILE(R.string.nav_profile, R.drawable.ic_user, isAvailable = true, inDrawer = false);

    val hasSubmenu: Boolean get() = opens != null

    /** The sub-menus that must be open to see this entry, e.g. FAQ → About and Settings. */
    val openGroups: Set<MenuGroup>
        get() {
            val groups = mutableSetOf<MenuGroup>()
            var current = group
            while (current != null) {
                val inside: MenuGroup = current
                groups += inside
                current = entries.first { it.opens == inside }.group
            }
            return groups
        }
}

/**
 * Shell of the signed-in app: top bar + side drawer (the web sidebar). Section screens get the menu
 * button; sub-screens pass [onBack] and get a back arrow and no drawer. Both show the account menu
 * when [user] is known, like the web header.
 */
@Composable
fun WorkspaceScaffold(
    user: UserBadge?,
    currentSection: WorkspaceSection,
    onSectionClick: (WorkspaceSection) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = XpTheme.colors
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    SessionTimeoutEffect()

    val scaffold = @Composable {
        Scaffold(
            modifier = modifier,
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                WorkspaceTopBar(
                    user = user,
                    onMenuClick = if (onBack == null) {
                        { scope.launch { drawerState.open() } }
                    } else {
                        null
                    },
                    onBack = onBack,
                    // A sub-screen isn't its section's page: My passwords from a password's details opens the list.
                    onSectionClick = { section -> if (onBack != null || section != currentSection) onSectionClick(section) },
                )
            },
            content = content,
        )
    }

    if (onBack != null) {
        scaffold()
        return
    }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                user = user,
                currentSection = currentSection,
                onSectionClick = { section ->
                    scope.launch { drawerState.close() }
                    if (section != currentSection) onSectionClick(section)
                },
            )
        },
        content = scaffold,
    )
}

@Composable
private fun WorkspaceTopBar(
    user: UserBadge?,
    onMenuClick: (() -> Unit)?,
    onBack: (() -> Unit)?,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    Column(modifier = Modifier.background(colors.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(60.dp)
                .padding(start = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                onBack != null -> IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.back),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                onMenuClick != null -> IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.cd_open_menu),
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            BrandLogo(modifier = Modifier.padding(start = 4.dp))
            Spacer(Modifier.weight(1f))
            ThemeToggleButton()
            if (user != null) {
                Spacer(Modifier.width(8.dp))
                AccountMenu(user = user, onSectionClick = onSectionClick)
            }
        }
        HorizontalDivider(color = colors.divider)
    }
}

/** The web header's theme button: a sun while light, a moon while dark; a tap switches to the other. */
@Composable
private fun ThemeToggleButton() {
    val colors = XpTheme.colors
    val toggleTheme = LocalThemeToggle.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.divider, shape)
            .clickable(role = Role.Button) { toggleTheme(colors.isDark) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (colors.isDark) R.drawable.ic_moon else R.drawable.ic_sun),
            contentDescription = stringResource(R.string.cd_toggle_theme),
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.xprokey_logo),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.brand_name),
            style = XpTheme.typography.brand.copy(fontSize = 16.sp),
            color = XpTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun Avatar(user: UserBadge, modifier: Modifier = Modifier, size: Int = 34) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(XpTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = user.initials,
            style = XpTheme.typography.bodyBold.copy(fontSize = if (size >= 40) 13.sp else 12.sp),
            color = XpTheme.colors.onPrimary,
        )
    }
}

/**
 * The web header's account menu, opened from the avatar: who is signed in, View profile, My
 * passwords, Account settings (the Security page, like the web) and Log out.
 */
@Composable
private fun AccountMenu(user: UserBadge, onSectionClick: (WorkspaceSection) -> Unit) {
    val colors = XpTheme.colors
    val logOut = LocalLogOut.current
    var expanded by remember { mutableStateOf(false) }
    val open = { section: WorkspaceSection ->
        expanded = false
        onSectionClick(section)
    }
    Box {
        Avatar(
            user = user,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_account_menu)) { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.surface,
            border = BorderStroke(1.dp, colors.divider),
            modifier = Modifier.width(256.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(user = user, size = 40)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.displayName,
                            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = user.email,
                            style = XpTheme.typography.body.copy(fontSize = 10.5.sp),
                            color = colors.textLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                AccountMenuItem(R.drawable.ic_user, stringResource(R.string.account_view_profile)) { open(WorkspaceSection.PROFILE) }
                AccountMenuItem(R.drawable.ic_key_round, stringResource(R.string.account_my_passwords)) { open(WorkspaceSection.PASSWORDS) }
                AccountMenuItem(R.drawable.ic_settings, stringResource(R.string.account_settings)) { open(WorkspaceSection.SECURITY) }
                HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 10.dp))
                AccountMenuItem(R.drawable.ic_log_out, stringResource(R.string.account_log_out), isDanger = true) {
                    expanded = false
                    logOut()
                }
            }
        }
    }
}

@Composable
private fun AccountMenuItem(@DrawableRes icon: Int, text: String, isDanger: Boolean = false, onClick: () -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isDanger) colors.error else colors.textLabel,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = if (isDanger) colors.error else colors.textSecondary,
        )
    }
}

@Composable
private fun WorkspaceDrawer(
    user: UserBadge?,
    currentSection: WorkspaceSection,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    ModalDrawerSheet(
        drawerContainerColor = colors.surface,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        windowInsets = WindowInsets.systemBars,
        modifier = Modifier.width(290.dp),
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            BrandLogo(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp))
            HorizontalDivider(color = colors.divider)
            Text(
                text = stringResource(R.string.menu).uppercase(),
                style = XpTheme.typography.mono.copy(fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold),
                color = colors.textMuted,
                modifier = Modifier.padding(start = 22.dp, top = 20.dp, bottom = 10.dp),
            )
            // Like the web sidebar: Tools, Settings and About open and close their lists, and the
            // ones holding the current page start open.
            var openGroups by rememberSaveable { mutableStateOf(currentSection.openGroups) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MenuEntries(
                    group = null,
                    depth = 0,
                    currentSection = currentSection,
                    openGroups = openGroups,
                    onToggle = { group -> openGroups = if (group in openGroups) openGroups - group else openGroups + group },
                    onSectionClick = onSectionClick,
                )
            }
            if (user != null) {
                HorizontalDivider(color = colors.divider)
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(user)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.displayName,
                            style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = user.email,
                            style = XpTheme.typography.body.copy(fontSize = 11.5.sp),
                            color = colors.textLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** The entries of [group] (null = top level), each open sub-menu indented below its entry. */
@Composable
private fun MenuEntries(
    group: MenuGroup?,
    depth: Int,
    currentSection: WorkspaceSection,
    openGroups: Set<MenuGroup>,
    onToggle: (MenuGroup) -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    WorkspaceSection.entries.filter { it.group == group && it.inDrawer }.forEach { section ->
        val opens = section.opens
        val isOpen = opens != null && opens in openGroups
        DrawerItem(
            section = section,
            selected = section == currentSection,
            depth = depth,
            isExpanded = isOpen,
            onClick = { if (opens != null) onToggle(opens) else onSectionClick(section) },
        )
        if (opens != null && isOpen) {
            MenuEntries(opens, depth + 1, currentSection, openGroups, onToggle, onSectionClick)
        }
    }
}

@Composable
private fun DrawerItem(
    section: WorkspaceSection,
    selected: Boolean,
    depth: Int,
    onClick: () -> Unit,
    isExpanded: Boolean = false,
) {
    val colors = XpTheme.colors
    val tint = if (selected) colors.primary else colors.textSecondary
    Row(
        modifier = Modifier
            // The web indents sub-menus by 24 px, and About's items by 20 px more.
            .padding(start = if (depth == 0) 0.dp else (4 + 20 * depth).dp)
            .fillMaxWidth()
            .height(if (depth == 0) 46.dp else 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.surface)
            .clickable(enabled = section.isAvailable, role = Role.Button, onClick = onClick)
            .alpha(if (section.isAvailable) 1f else 0.45f)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(section.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(
                when (depth) {
                    0 -> 18.dp
                    1 -> 15.dp
                    else -> 13.dp
                }
            ),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(section.title),
            style = XpTheme.typography.navItem.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
            color = if (selected) colors.primary else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        if (section.hasSubmenu) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (isExpanded) 180f else 0f),
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun WorkspaceDrawerPreview() {
    XproKeyTheme(darkTheme = true) {
        WorkspaceDrawer(
            user = UserBadge.from(name = "Vansh Goel", email = "goelvansh770@gmail.com"),
            currentSection = WorkspaceSection.CARDS,
            onSectionClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkspaceTopBarPreview() {
    XproKeyTheme(darkTheme = false) {
        WorkspaceScaffold(
            user = UserBadge.from(name = "Vansh Goel", email = "goelvansh770@gmail.com"),
            currentSection = WorkspaceSection.DASHBOARD,
            onSectionClick = {},
            snackbarHostState = remember { SnackbarHostState() },
        ) {}
    }
}
