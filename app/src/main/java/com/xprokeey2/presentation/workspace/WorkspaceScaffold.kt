package com.xprokeey2.presentation.workspace

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import kotlinx.coroutines.launch

/** Sidebar sections of the web app. Only the ones in [WorkspaceSection.isAvailable] work so far. */
enum class WorkspaceSection(
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
    val isAvailable: Boolean,
    val hasSubmenu: Boolean = false,
) {
    DASHBOARD(R.string.nav_dashboard, R.drawable.ic_layout_grid, isAvailable = true),
    PASSWORDS(R.string.nav_passwords, R.drawable.ic_lock, isAvailable = false),
    CARDS(R.string.nav_cards, R.drawable.ic_credit_card, isAvailable = true),
    TOOLS(R.string.nav_tools, R.drawable.ic_wrench, isAvailable = false, hasSubmenu = true),
    SETTINGS(R.string.nav_settings, R.drawable.ic_settings, isAvailable = false, hasSubmenu = true),
    SUPPORT(R.string.nav_support, R.drawable.ic_headphones, isAvailable = false),
}

/**
 * Shell of the signed-in app: top bar + side drawer (the web sidebar). Section screens get the menu
 * button; sub-screens pass [onBack] and get a back arrow and no drawer.
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
            if (user != null) Avatar(user)
        }
        HorizontalDivider(color = colors.divider)
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
private fun Avatar(user: UserBadge, size: Int = 34) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(XpTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = user.initials,
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
            color = XpTheme.colors.onPrimary,
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
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WorkspaceSection.entries.forEach { section ->
                    DrawerItem(
                        section = section,
                        selected = section == currentSection,
                        onClick = { onSectionClick(section) },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
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

@Composable
private fun DrawerItem(section: WorkspaceSection, selected: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val tint = if (selected) colors.primary else colors.textSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
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
            modifier = Modifier.size(18.dp),
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
                modifier = Modifier.size(14.dp),
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
