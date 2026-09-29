package com.xprokeey2.presentation.tools.importdata

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.PickedFile
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ImportScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ImportEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is ImportEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    // Any file: like the web, the extension (.csv, .json, .xpk) decides how it's read.
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onAction(ImportAction.FilePicked(uri.toString()))
    }

    ImportScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSectionClick = onSectionClick,
        onPickFile = {
            try {
                openFile.launch(arrayOf("*/*"))
            } catch (e: ActivityNotFoundException) {
                viewModel.onAction(ImportAction.PickerUnavailable)
            }
        },
        onImport = { viewModel.onAction(ImportAction.Import) },
    )
}

@Composable
fun ImportScreen(
    state: ImportUiState,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onPickFile: () -> Unit,
    onImport: () -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.IMPORT,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImportBadge()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.import_title),
                style = XpTheme.typography.headline,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            FileBox(file = state.file, enabled = !state.isImporting, onClick = onPickFile)

            Spacer(Modifier.height(20.dp))
            ImportButton(
                isImporting = state.isImporting,
                enabled = state.file != null && !state.isImporting,
                onClick = onImport,
            )

            Spacer(Modifier.height(20.dp))
            RequiredFields()
        }
    }
}

@Composable
private fun ImportBadge() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, shape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_upload),
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.import_badge),
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            color = colors.primary,
        )
    }
}

/** The web's drop area: tap to choose a file; shows the chosen file's name and size. */
@Composable
private fun FileBox(file: PickedFile?, enabled: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val dashColor = colors.fieldBorder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .drawBehind {
                val stroke = 2.dp.toPx()
                inset(stroke / 2) {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                        ),
                    )
                }
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (file != null) {
            FileTile(icon = R.drawable.ic_check_circle, tint = colors.success)
            Spacer(Modifier.height(12.dp))
            Text(
                text = file.name,
                style = XpTheme.typography.bodyBold.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                // `(file.size / 1024).toFixed(2) KB`
                text = stringResource(R.string.import_file_size, String.format(Locale.US, "%.2f", file.sizeBytes / 1024.0)),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.fieldBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        } else {
            FileTile(icon = R.drawable.ic_file_up, tint = colors.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.import_choose_file),
                style = XpTheme.typography.bodyBold.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.import_browse),
                style = XpTheme.typography.bodyBold.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.import_formats),
                style = XpTheme.typography.body.copy(fontSize = 12.sp),
                color = colors.textLabel,
            )
        }
    }
}

@Composable
private fun FileTile(@DrawableRes icon: Int, tint: Color) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = if (XpTheme.colors.isDark) 0.16f else 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun ImportButton(isImporting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (enabled || isImporting) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primary)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_upload),
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (isImporting) R.string.import_importing else R.string.import_button),
            style = XpTheme.typography.button,
            color = colors.onPrimary,
        )
    }
}

@Composable
private fun RequiredFields() {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.divider, shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.import_required_title),
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.import_required_body),
                style = XpTheme.typography.body.copy(fontSize = 12.sp),
                color = colors.textSecondary,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ImportScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        ImportScreen(
            state = ImportUiState(user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com")),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onPickFile = {},
            onImport = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ImportScreenFilePreview() {
    XproKeyTheme(darkTheme = true) {
        ImportScreen(
            state = ImportUiState(
                user = UserBadge.from("Vansh Goel", "goelv2610@gmail.com"),
                file = PickedFile(uri = "content://x", name = "XproKey (1).xpk", sizeBytes = 1311),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onPickFile = {},
            onImport = {},
        )
    }
}
