package com.xprokeey2.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpLogoHeader
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme

/** Temporary placeholder, to be replaced by the real dashboard. */
@Composable
fun DashboardScreen(organization: String?) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val cardShape = RoundedCornerShape(20.dp)

    AuthScreenLayout(
        snackbarHostState = remember { SnackbarHostState() },
        horizontalAlignment = Alignment.CenterHorizontally,
        containerColor = colors.background,
    ) {
        XpLogoHeader()
        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.divider, cardShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = typography.headline,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dashboard_placeholder),
                style = typography.subtitle,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (organization != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_organization, organization),
                    style = typography.bodyBold,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    XproKeyTheme(darkTheme = true) { DashboardScreen(organization = "Acme Corp") }
}
