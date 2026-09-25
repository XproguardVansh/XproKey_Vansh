package com.xprokeey2.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xprokeey2.R
import com.xprokeey2.domain.model.PasswordStrength
import com.xprokeey2.presentation.theme.XpTheme

/** Four segments, one per scoring criterion (tech doc §2); colours follow the doc's rating table. */
@Composable
fun PasswordStrengthMeter(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    val colors = XpTheme.colors
    val (activeColor, label) = when (strength.level) {
        PasswordStrength.Level.WEAK -> colors.error to R.string.strength_weak
        PasswordStrength.Level.OKAY -> colors.warning to R.string.strength_okay
        PasswordStrength.Level.GOOD -> colors.primary to R.string.strength_good
        PasswordStrength.Level.STRONG -> colors.success to R.string.strength_strong
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(PasswordStrength.MAX_SCORE) { index ->
                val segmentColor by animateColorAsState(
                    targetValue = if (index < strength.score) activeColor else colors.fieldBorder,
                    label = "strengthSegment",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(segmentColor, RoundedCornerShape(3.dp)),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(label),
            style = XpTheme.typography.caption,
            color = activeColor,
        )
    }
}
