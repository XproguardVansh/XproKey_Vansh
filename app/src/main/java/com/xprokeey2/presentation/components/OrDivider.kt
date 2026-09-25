package com.xprokeey2.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme

@Composable
fun OrDivider(modifier: Modifier = Modifier) {
    val colors = XpTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
        Text(
            text = stringResource(R.string.or),
            style = XpTheme.typography.divider,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = colors.divider)
    }
}
