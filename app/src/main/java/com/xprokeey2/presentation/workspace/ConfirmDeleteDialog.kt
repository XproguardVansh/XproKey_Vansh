package com.xprokeey2.presentation.workspace

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme

/** "Delete …?" confirmation with a red Delete button that spins while [isDeleting]. */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = XpTheme.colors
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = colors.surface,
        title = { Text(text = title, style = XpTheme.typography.dialogTitle, color = colors.textPrimary) },
        text = { Text(text = body, style = XpTheme.typography.dialogBody, color = colors.textSecondary) },
        confirmButton = {
            CompactButton(
                text = stringResource(R.string.delete),
                icon = R.drawable.ic_trash,
                onClick = onConfirm,
                style = CompactButtonStyle.Danger,
                isLoading = isDeleting,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = XpTheme.typography.buttonSecondary,
                    color = colors.textSecondary,
                )
            }
        },
    )
}
