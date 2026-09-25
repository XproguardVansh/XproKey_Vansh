package com.xprokeey2.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xprokeey2.R
import com.xprokeey2.presentation.theme.XpTheme

@Composable
fun XpLogoHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.xprokey_logo),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.brand_name),
            style = XpTheme.typography.brand,
            color = XpTheme.colors.textPrimary,
        )
    }
}
