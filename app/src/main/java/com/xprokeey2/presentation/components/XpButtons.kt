package com.xprokeey2.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xprokeey2.presentation.theme.XpTheme

private val ButtonShape = RoundedCornerShape(11.dp)

/** Filled primary button with the blue glow from Figma. Shows a spinner while [isLoading]. */
@Composable
fun XpPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = XpTheme.colors
    val clickable = enabled && !isLoading
    Box(
        modifier = modifier
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = ButtonShape,
                        ambientColor = colors.primary,
                        spotColor = colors.primary.copy(alpha = 0.6f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(ButtonShape)
            .background(if (enabled) colors.primary else colors.primary.copy(alpha = 0.45f))
            .clickable(enabled = clickable, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = XpTheme.typography.button,
                color = if (enabled) colors.onPrimary else colors.onPrimary.copy(alpha = 0.55f),
            )
        }
    }
}

/** Outlined surface button, e.g. "Continue with Google". */
@Composable
fun XpSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = XpTheme.colors
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(ButtonShape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = XpTheme.typography.buttonSecondary,
            color = colors.textPrimary,
        )
    }
}
