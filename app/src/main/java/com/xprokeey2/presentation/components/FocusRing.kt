package com.xprokeey2.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Soft 3dp halo drawn outside the bounds of a focused input (Figma: 0 0 0 3px primary @ 12%). */
fun Modifier.focusRing(
    visible: Boolean,
    color: Color,
    cornerRadius: Dp,
    width: Dp = 3.dp,
): Modifier = drawBehind {
    if (visible) {
        val ring = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(-ring, -ring),
            size = Size(size.width + ring * 2, size.height + ring * 2),
            cornerRadius = CornerRadius(cornerRadius.toPx() + ring),
        )
    }
}
