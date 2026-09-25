package com.xprokeey2.presentation.auth.verify.components

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xprokeey2.R
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.components.XpSecondaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown once, right after the account is verified. It can't be dismissed by tapping
 * outside or pressing back: the user must explicitly continue after saving the key.
 */
@Composable
fun RecoveryKeyDialog(
    email: String,
    recoveryKey: String,
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val saveCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = writeRecoveryCsv(context, uri, email, recoveryKey)
                val message = if (saved) R.string.recovery_key_saved else R.string.recovery_key_save_failed
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        RecoveryKeyDialogContent(
            recoveryKey = recoveryKey,
            onCopy = {
                scope.launch {
                    val clip = ClipData.newPlainText(context.getString(R.string.recovery_key_clip_label), recoveryKey)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        // Keeps the key out of the clipboard preview/overlay on Android 13+.
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                    clipboard.setClipEntry(ClipEntry(clip))
                    // Android 13+ shows its own copy confirmation.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(context, R.string.recovery_key_copied, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDownloadCsv = { saveCsvLauncher.launch(RECOVERY_FILE_NAME) },
            onContinue = onContinue,
        )
    }
}

@Composable
private fun RecoveryKeyDialogContent(
    recoveryKey: String,
    onCopy: () -> Unit,
    onDownloadCsv: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val warningShape = RoundedCornerShape(12.dp)
    val keyShape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(18.dp))
            .border(1.dp, colors.fieldBorder, RoundedCornerShape(18.dp))
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.recovery_title),
            style = typography.dialogTitle,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.recovery_body),
            style = typography.dialogBody,
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.warning.copy(alpha = 0.10f), warningShape)
                .border(1.dp, colors.warning.copy(alpha = 0.45f), warningShape)
                .padding(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alert_triangle),
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.recovery_warning),
                style = typography.body,
                color = colors.warning,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = recoveryKey,
            style = typography.mono,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.fieldBackground, keyShape)
                .border(1.dp, colors.fieldBorder, keyShape)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        )

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XpSecondaryButton(
                text = stringResource(R.string.copy),
                onClick = onCopy,
                modifier = Modifier.weight(1f),
            )
            XpSecondaryButton(
                text = stringResource(R.string.download_csv),
                onClick = onDownloadCsv,
                modifier = Modifier.weight(1.4f),
            )
        }
        Spacer(Modifier.height(12.dp))
        XpPrimaryButton(
            text = stringResource(R.string.continue_label),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val RECOVERY_FILE_NAME = "xprokey-recovery-key.csv"

private suspend fun writeRecoveryCsv(
    context: Context,
    uri: Uri,
    email: String,
    recoveryKey: String,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val csv = "email,recovery_key\n$email,$recoveryKey\n"
        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open $uri")
    }.isSuccess
}

@Preview(showBackground = true)
@Composable
private fun RecoveryKeyDialogPreview() {
    XproKeyTheme(darkTheme = true) {
        RecoveryKeyDialogContent(
            recoveryKey = "nKa9bXmzrOzVIPoW9Y4gBNbvvt8lmKqr+gOBcO4xGos=",
            onCopy = {},
            onDownloadCsv = {},
            onContinue = {},
        )
    }
}
