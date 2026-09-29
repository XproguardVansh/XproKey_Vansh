package com.xprokeey2.presentation.tools.exportdata

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.ExportFormat
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch

@Composable
fun ExportScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ExportEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is ExportEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    // Android's "Save as" screen, one per file type so the right MIME type is used.
    val saveAs = ExportFormat.entries.associateWith { format ->
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mimeType)) { uri ->
            if (uri != null) viewModel.onAction(ExportAction.SaveTo(format, uri.toString()))
        }
    }

    ExportScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSectionClick = onSectionClick,
        onDownload = { format ->
            try {
                saveAs.getValue(format).launch(format.fileName)
            } catch (e: ActivityNotFoundException) {
                viewModel.onAction(ExportAction.SaveScreenUnavailable)
            }
        },
    )
}

@Composable
fun ExportScreen(
    state: ExportUiState,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onDownload: (ExportFormat) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.EXPORT,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.export_title),
                    style = XpTheme.typography.pageTitle,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                ExportBadge()
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.export_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormatCard(
                    icon = R.drawable.ic_download,
                    title = stringResource(R.string.export_csv_title),
                    body = stringResource(R.string.export_csv_body),
                    features = listOf(
                        stringResource(R.string.export_csv_feature_1),
                        stringResource(R.string.export_csv_feature_2),
                        stringResource(R.string.export_csv_feature_3),
                    ),
                    buttonText = stringResource(R.string.export_csv_button),
                    accent = FormatAccent.Sky,
                    isPreparing = ExportFormat.CSV in state.preparing,
                    onDownload = { onDownload(ExportFormat.CSV) },
                )
                FormatCard(
                    icon = R.drawable.ic_file_json,
                    title = stringResource(R.string.export_json_title),
                    body = stringResource(R.string.export_json_body),
                    features = listOf(
                        stringResource(R.string.export_json_feature_1),
                        stringResource(R.string.export_json_feature_2),
                        stringResource(R.string.export_json_feature_3),
                    ),
                    buttonText = stringResource(R.string.export_json_button),
                    accent = FormatAccent.Violet,
                    isPreparing = ExportFormat.JSON in state.preparing,
                    onDownload = { onDownload(ExportFormat.JSON) },
                )
                FormatCard(
                    icon = R.drawable.ic_shield_check,
                    title = stringResource(R.string.export_xpk_title),
                    body = stringResource(R.string.export_xpk_body),
                    features = listOf(
                        stringResource(R.string.export_xpk_feature_1),
                        stringResource(R.string.export_xpk_feature_2),
                        stringResource(R.string.export_xpk_feature_3),
                    ),
                    buttonText = stringResource(R.string.export_xpk_button),
                    accent = FormatAccent.Emerald,
                    isPreparing = ExportFormat.ENCRYPTED in state.preparing,
                    onDownload = { onDownload(ExportFormat.ENCRYPTED) },
                )
            }

            Spacer(Modifier.height(24.dp))
            Notice(
                icon = R.drawable.ic_alert_triangle,
                iconTint = if (colors.isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                title = stringResource(R.string.export_keep_safe_title),
                titleColor = if (colors.isDark) Color(0xFFFBBF24) else colors.textPrimary,
                body = stringResource(R.string.export_keep_safe_body),
                background = if (colors.isDark) colors.surface else Color(0xFFFFFBEB).copy(alpha = 0.5f),
                border = if (colors.isDark) colors.divider else Color(0xFFFEF3C7),
            )
            Spacer(Modifier.height(12.dp))
            Notice(
                icon = R.drawable.ic_info,
                iconTint = colors.primary,
                title = stringResource(R.string.export_regular_title),
                titleColor = colors.textPrimary,
                body = stringResource(R.string.export_regular_body),
                background = if (colors.isDark) colors.surface else Color(0xFFEFF6FF).copy(alpha = 0.5f),
                border = if (colors.isDark) colors.divider else Color(0xFFDBEAFE),
            )
        }
    }
}

/** Card colours of the web page: sky (CSV), violet (JSON), emerald (.xpk). */
private enum class FormatAccent(val light: Color, val dark: Color) {
    Sky(Color(0xFF0284C7), Color(0xFF38BDF8)),
    Violet(Color(0xFF7C3AED), Color(0xFFA78BFA)),
    Emerald(Color(0xFF059669), Color(0xFF34D399)),
}

@Composable
private fun ExportBadge() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = stringResource(R.string.export_badge).uppercase(),
        style = XpTheme.typography.caption.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
        color = colors.primary,
        modifier = Modifier
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f))
            .border(1.dp, colors.primary.copy(alpha = 0.15f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun FormatCard(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    features: List<String>,
    buttonText: String,
    accent: FormatAccent,
    isPreparing: Boolean,
    onDownload: () -> Unit,
) {
    val colors = XpTheme.colors
    val tint = if (colors.isDark) accent.dark else accent.light
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, accent.light.copy(alpha = 0.2f), shape)
            .padding(22.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.copy(alpha = if (colors.isDark) 0.16f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = XpTheme.typography.sectionTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary.copy(alpha = 0.85f),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.light)
                .clickable(enabled = !isPreparing, role = Role.Button, onClick = onDownload)
                .alpha(if (isPreparing) 0.6f else 1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isPreparing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            } else {
                Icon(painter = painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isPreparing) stringResource(R.string.export_preparing) else buttonText,
                style = XpTheme.typography.button.copy(fontSize = 13.sp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun Notice(
    @DrawableRes icon: Int,
    iconTint: Color,
    title: String,
    titleColor: Color,
    body: String,
    background: Color,
    border: Color,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp), color = titleColor)
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = XpTheme.typography.body.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                color = XpTheme.colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ExportScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        ExportScreen(
            state = ExportUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                preparing = setOf(ExportFormat.JSON),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onDownload = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ExportScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        ExportScreen(
            state = ExportUiState(user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com")),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onDownload = {},
        )
    }
}
