package com.xprokeey2.presentation.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.xprokeey2.presentation.theme.XpTheme

/** "Remember your password? **Log in**"-style footer link. */
@Composable
fun AuthLinkText(
    prefix: String,
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = XpTheme.colors
    Text(
        text = buildAnnotatedString {
            append(prefix)
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary)) { append(link) }
        },
        style = XpTheme.typography.body,
        color = colors.textSecondary,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
    )
}
