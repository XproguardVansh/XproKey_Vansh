package com.xprokeey2.presentation.onboarding.plan

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xprokeey2.R
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpPrimaryButton
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme

private enum class Plan(
    @param:StringRes val title: Int,
    @param:StringRes val tagline: Int,
    @param:StringRes val period: Int,
    val isRecommended: Boolean,
) {
    QUARTERLY(R.string.plan_quarterly, R.string.plan_quarterly_tagline, R.string.plan_quarterly_period, isRecommended = false),
    YEARLY(R.string.plan_yearly, R.string.plan_yearly_tagline, R.string.plan_yearly_period, isRecommended = true),
}

private val PlanFeatures = listOf(
    R.string.feature_unlimited_items,
    R.string.feature_encrypted_vaults,
    R.string.feature_generator_tools,
    R.string.feature_zero_knowledge,
)

/**
 * "Choose your plan" for Personal accounts. Payment (Razorpay) isn't built yet, so the plans can
 * be compared and selected but the trial button stays disabled.
 */
@Composable
fun PlanScreen(onBack: () -> Unit) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    var selected by rememberSaveable { mutableStateOf(Plan.YEARLY) }

    AuthScreenLayout(
        snackbarHostState = remember { SnackbarHostState() },
        horizontalAlignment = Alignment.CenterHorizontally,
        containerColor = colors.background,
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.back),
                style = typography.bodyBold.copy(fontSize = 12.5.sp),
                color = colors.onPrimary,
            )
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.primary.copy(alpha = 0.10f))
                .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield_check),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.plan_secure_payment).uppercase(),
                style = typography.caption.copy(fontSize = 9.5.sp, letterSpacing = 0.6.sp),
                color = colors.primary,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.plan_title),
            style = typography.headline,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.plan_subtitle),
            style = typography.subtitle,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Plan.entries.forEach { plan ->
                PlanCard(plan = plan, selected = plan == selected, onSelect = { selected = plan })
            }
        }

        Spacer(Modifier.height(28.dp))
        XpPrimaryButton(
            text = stringResource(R.string.plan_start_trial),
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onSelect: () -> Unit) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography
    val shape = RoundedCornerShape(20.dp)

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.divider, shape)
                .clickable(role = Role.RadioButton, onClick = onSelect)
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(plan.title),
                style = typography.headline.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(plan.tagline),
                style = typography.body.copy(fontSize = 12.5.sp),
                color = colors.textLabel,
            )

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.fieldBackground)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.plan_price),
                    style = typography.statValue,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(plan.period),
                    style = typography.bodyBold.copy(fontSize = 12.5.sp),
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.plan_trial_badge),
                    style = typography.bodyBold.copy(fontSize = 11.5.sp),
                    color = colors.primary,
                )
            }

            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanFeatures.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(feature),
                            style = typography.bodyBold.copy(fontSize = 13.sp),
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colors.primary.copy(alpha = 0.10f) else colors.fieldBackground)
                    .border(1.dp, if (selected) colors.primary else colors.fieldBorder, RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onSelect),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (selected) R.string.plan_selected else R.string.plan_click_to_select).uppercase(),
                    style = typography.bodyBold.copy(fontSize = 12.sp, letterSpacing = 0.5.sp),
                    color = if (selected) colors.primary else colors.textSecondary,
                )
            }
        }

        if (plan.isRecommended) {
            Text(
                text = stringResource(R.string.plan_recommended).uppercase(),
                style = typography.caption.copy(fontSize = 9.5.sp, letterSpacing = 0.8.sp),
                color = colors.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(colors.primary)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 1500)
@Composable
private fun PlanScreenPreview() {
    XproKeyTheme(darkTheme = false) { PlanScreen(onBack = {}) }
}

@Preview(name = "Dark", showBackground = true, heightDp = 1500)
@Composable
private fun PlanScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) { PlanScreen(onBack = {}) }
}
