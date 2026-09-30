package com.xprokeey2.presentation.onboarding.plan

import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.SubscriptionPlan
import com.xprokeey2.presentation.auth.components.AuthScreenLayout
import com.xprokeey2.presentation.components.XpActionButton
import com.xprokeey2.presentation.payment.PaymentResult
import com.xprokeey2.presentation.payment.RazorpayCheckout
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import kotlinx.coroutines.launch

/** How each plan is shown; the prices are the web's (the API doesn't send them). */
private enum class PlanOption(
    val plan: SubscriptionPlan,
    @param:StringRes val title: Int,
    @param:StringRes val tagline: Int,
    @param:StringRes val period: Int,
    val isRecommended: Boolean,
) {
    QUARTERLY(SubscriptionPlan.QUARTERLY, R.string.plan_quarterly, R.string.plan_quarterly_tagline, R.string.plan_quarterly_period, isRecommended = false),
    YEARLY(SubscriptionPlan.YEARLY, R.string.plan_yearly, R.string.plan_yearly_tagline, R.string.plan_yearly_period, isRecommended = true),
}

private val PlanFeatures = listOf(
    R.string.feature_unlimited_items,
    R.string.feature_encrypted_vaults,
    R.string.feature_generator_tools,
    R.string.feature_zero_knowledge,
)

@Composable
fun PlanScreenRoot(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    onAlreadyActive: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Loads Razorpay Checkout ahead of time so the payment sheet opens faster.
    LaunchedEffect(Unit) { RazorpayCheckout.preload(context) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PlanEvent.ShowMessage -> scope.launch {
                // The next step's message replaces the last one instead of waiting behind it.
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(event.message.asString(context))
            }
            is PlanEvent.OpenCheckout -> {
                val opened = activity != null && runCatching {
                    RazorpayCheckout.open(activity, event.subscriptionId, event.description.asString(context))
                }.isSuccess
                if (!opened) viewModel.onAction(PlanAction.PaymentFinished(PaymentResult.Failed(description = null)))
            }
            PlanEvent.Finished -> onFinished()
            is PlanEvent.AlreadyActive -> onAlreadyActive(event.message.asString(context))
            is PlanEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }

    PlanScreen(state = state, snackbarHostState = snackbarHostState, onAction = viewModel::onAction, onBack = onBack)
}

/**
 * "Choose your plan" for Personal accounts, like the web checkout: the 7-day free trial when the server
 * allows it, otherwise payment with Razorpay.
 */
@Composable
fun PlanScreen(
    state: PlanUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (PlanAction) -> Unit,
    onBack: () -> Unit,
) {
    val colors = XpTheme.colors
    val typography = XpTheme.typography

    if (state.isCheckingAccess) {
        CheckingAccess(snackbarHostState)
        return
    }

    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
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
            PlanOption.entries.forEach { option ->
                PlanCard(
                    option = option,
                    selected = option.plan == state.selectedPlan,
                    onSelect = { onAction(PlanAction.PlanSelected(option.plan)) },
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        XpActionButton(
            text = when (state.trialEligible) {
                null -> stringResource(R.string.checkout_loading)
                true -> stringResource(R.string.plan_start_trial)
                false -> stringResource(R.string.checkout_continue_to_payment)
            },
            loadingText = stringResource(R.string.checkout_please_wait),
            isLoading = state.isLoading,
            enabled = state.trialEligible != null,
            onClick = { onAction(PlanAction.Continue) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The web's "Checking subscription status…" page while billing and the trial are checked. */
@Composable
private fun CheckingAccess(snackbarHostState: SnackbarHostState) {
    val colors = XpTheme.colors
    AuthScreenLayout(
        snackbarHostState = snackbarHostState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        containerColor = colors.background,
    ) {
        CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.checkout_checking),
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun PlanCard(option: PlanOption, selected: Boolean, onSelect: () -> Unit) {
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
                text = stringResource(option.title),
                style = typography.headline.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(option.tagline),
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
                    text = stringResource(option.period),
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

        if (option.isRecommended) {
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

@Preview(name = "Light, trial", showBackground = true, heightDp = 1500)
@Composable
private fun PlanScreenPreview() {
    XproKeyTheme(darkTheme = false) {
        PlanScreen(
            state = PlanUiState(isCheckingAccess = false, trialEligible = true),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Dark, payment", showBackground = true, heightDp = 1500)
@Composable
private fun PlanScreenDarkPreview() {
    XproKeyTheme(darkTheme = true) {
        PlanScreen(
            state = PlanUiState(isCheckingAccess = false, trialEligible = false),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
        )
    }
}
