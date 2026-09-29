package com.xprokeey2.presentation.about

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.BuildConfig
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.workspace.IconTile
import com.xprokeey2.presentation.workspace.PageBadge
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection

@Composable
fun AppInfoScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    AppInfoScreen(user = user, version = BuildConfig.VERSION_NAME, onSectionClick = onSectionClick)
}

/** Settings > About > App Info: name, this app's version and the platform. */
@Composable
fun AppInfoScreen(
    user: UserBadge?,
    version: String,
    onSectionClick: (WorkspaceSection) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = user,
        currentSection = WorkspaceSection.APP_INFO,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_info_title),
                    style = XpTheme.typography.pageTitle,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(8.dp))
                PageBadge(stringResource(R.string.app_info_title))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.app_info_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = R.drawable.ic_package, tint = colors.primary, size = 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.app_info_details).uppercase(),
                        style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp, letterSpacing = 1.sp),
                        color = colors.textPrimary,
                    )
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = colors.divider)
                InfoRow(label = stringResource(R.string.app_info_name), value = stringResource(R.string.app_info_name_value))
                HorizontalDivider(color = colors.divider)
                InfoRow(label = stringResource(R.string.app_info_version), value = version)
                HorizontalDivider(color = colors.divider)
                InfoRow(
                    label = stringResource(R.string.app_info_platform),
                    value = stringResource(R.string.app_info_platform_value),
                    icon = R.drawable.ic_globe,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, @DrawableRes icon: Int? = null) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.textLabel,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
            color = colors.textPrimary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppInfoScreenPreview() {
    XproKeyTheme(darkTheme = true) {
        AppInfoScreen(user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"), version = "1.0", onSectionClick = {})
    }
}
