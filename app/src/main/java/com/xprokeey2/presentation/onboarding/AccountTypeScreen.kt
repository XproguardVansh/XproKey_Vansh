package com.xprokeey2.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme

private val CardShape = RoundedCornerShape(20.dp)

private val Features = listOf(
    R.string.feature_unlimited_items,
    R.string.feature_encrypted_keychains,
    R.string.feature_generator_tools,
    R.string.feature_zero_knowledge,
)

/** "Choose how you want to use Xprokey", shown after login until the account is set up. */
@Composable
fun AccountTypeScreen(onPersonalClick: () -> Unit, onBusinessClick: () -> Unit) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography

    AuthScreenLayout(
        snackbarHostState = remember { SnackbarHostState() },
        horizontalAlignment = Alignment.CenterHorizontally,
        containerColor = colors.background,
    ) {
        Text(
            text = stringResource(R.string.account_type_title),
            style = typography.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.account_type_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        AccountTypeCard(
            icon = R.drawable.ic_user,
            title = R.string.personal_account,
            description = R.string.personal_account_description,
            buttonText = R.string.buy_personal_plan,
            onClick = onPersonalClick,
            highlighted = false,
        )

        Spacer(Modifier.height(16.dp))
        AccountTypeCard(
            icon = R.drawable.ic_business,
            title = R.string.business_account,
            description = R.string.business_account_description,
            buttonText = R.string.activate_license_key,
            onClick = onBusinessClick,
            highlighted = true,
        )
    }
}

@Composable
private fun AccountTypeCard(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes description: Int,
    @StringRes buttonText: Int,
    onClick: () -> Unit,
    highlighted: Boolean,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .border(1.dp, if (highlighted) colors.primary else colors.divider, CardShape)
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(title),
                style = typography.headline.copy(fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.4).sp),
                color = colors.textPrimary,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(description),
            style = typography.body.copy(fontSize = 14.sp, lineHeight = 21.sp),
            color = colors.textSecondary,
        )

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield_check),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(feature),
                        style = typography.body.copy(fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        XpPrimaryButton(
            text = stringResource(buttonText),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 1300)
@Composable
private fun AccountTypeScreenPreview() {
    XproKeyTheme(darkTheme = false) { AccountTypeScreen(onPersonalClick = {}, onBusinessClick = {}) }
}

@Preview(name = "Dark", showBackground = true, heightDp = 1300)
@Composable
private fun AccountTypeScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) { AccountTypeScreen(onPersonalClick = {}, onBusinessClick = {}) }
}
