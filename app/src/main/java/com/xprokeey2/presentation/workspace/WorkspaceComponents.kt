package com.xprokeey2.presentation.workspace

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.presentation.theme.XpTheme

val PanelShape = RoundedCornerShape(16.dp)
private val CompactShape = RoundedCornerShape(10.dp)

/** Rounded surface block used for every dashboard / cards section. */
@Composable
fun WorkspacePanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = XpTheme.colors
    Column(
        modifier = modifier
            .clip(PanelShape)
            .background(colors.surface)
            .border(1.dp, colors.divider, PanelShape)
            .padding(contentPadding),
        content = content,
    )
}

/** Big page heading with an optional button on the right ("Add card", "Add password"). */
@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = XpTheme.typography.pageTitle,
            color = XpTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/** Small uppercase tag next to a Tools page title ("EXPORT", "GENERATOR"). */
@Composable
fun PageBadge(text: String) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text.uppercase(),
        style = XpTheme.typography.caption.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
        color = colors.primary,
        modifier = Modifier
            .clip(shape)
            .background(colors.primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f))
            .border(1.dp, colors.primary.copy(alpha = 0.15f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

enum class CompactButtonStyle { Primary, Neutral, Danger }

/** Small button of the workspace screens: "+ Add card", "Reveal", "View", "Edit", "Delete". */
@Composable
fun CompactButton(
    text: String?,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CompactButtonStyle = CompactButtonStyle.Neutral,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = XpTheme.colors
    val (background, content, border) = when (style) {
        CompactButtonStyle.Primary -> Triple(colors.primary, colors.onPrimary, Color.Transparent)
        CompactButtonStyle.Danger -> Triple(colors.error, Color.White, Color.Transparent)
        CompactButtonStyle.Neutral -> Triple(colors.fieldBackground, colors.textSecondary, colors.fieldBorder)
    }
    Row(
        modifier = modifier
            .height(40.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CompactShape)
            .background(background)
            .border(1.dp, border, CompactShape)
            .clickable(enabled = enabled && !isLoading, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(horizontal = if (text == null) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = content, strokeWidth = 2.dp)
        } else if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = if (text == null) contentDescription else null,
                tint = content,
                modifier = Modifier.size(15.dp),
            )
        }
        if (text != null) {
            if (icon != null || isLoading) Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
                color = content,
                maxLines = 1,
            )
        }
    }
}

/** Tinted rounded square behind an icon (stat tiles, quick actions). */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.47f),
        )
    }
}

/** Centered spinner for a whole panel or screen that is still loading. */
@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = XpTheme.colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}
