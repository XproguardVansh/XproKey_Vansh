package com.xprokeey2.presentation.settings.billing

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.presentation.components.XpActionButton
import com.xprokeey2.presentation.settings.billingDate
import com.xprokeey2.presentation.settings.capitalizeFirst
import com.xprokeey2.presentation.settings.trialPurple
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.util.ResultMessageEffect
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun BillingScreenRoot(
    resultMessage: String?,
    onResultMessageShown: () -> Unit,
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onUpgrade: () -> Unit,
    onGetAccess: () -> Unit,
    onLoadFailed: (message: String) -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: BillingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is BillingEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is BillingEvent.LoadFailed -> onLoadFailed(event.message.asString(context))
            is BillingEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    ResultMessageEffect(resultMessage, snackbarHostState, onResultMessageShown)

    BillingScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onSectionClick = onSectionClick,
        onUpgrade = onUpgrade,
        onGetAccess = onGetAccess,
    )
}

/** Manage Subscription, laid out like the web's billing page. */
@Composable
fun BillingScreen(
    state: BillingUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (BillingAction) -> Unit,
    onBack: () -> Unit,
    onSectionClick: (WorkspaceSection) -> Unit,
    onUpgrade: () -> Unit,
    onGetAccess: () -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.SUBSCRIPTION,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.subscription_title),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.billing_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            val billing = state.billing
            if (billing == null) {
                if (state.isLoading) LoadingBlock()
            } else {
                PlanCard(state = state, billing = billing)
                Spacer(Modifier.height(20.dp))
                ManagementCard(state = state, billing = billing, onAction = onAction, onUpgrade = onUpgrade, onGetAccess = onGetAccess)
            }
        }
    }

    if (state.isCancelDialogVisible) {
        CancelDialog(
            accessUntil = state.billing?.nextBillingAt?.billingDate(),
            onConfirm = { onAction(BillingAction.ConfirmCancel) },
            onDismiss = { onAction(BillingAction.DismissCancelDialog) },
        )
    }
}

@Composable
private fun PlanCard(state: BillingUiState, billing: Billing) {
    val colors = XpTheme.colors
    val plan = billing.planType?.capitalizeFirst() ?: stringResource(R.string.billing_not_purchased)
    val nextBilling = billing.nextBillingAt?.billingDate()

    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val iconShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(colors.primary.copy(alpha = 0.10f))
                    .border(1.dp, colors.primary.copy(alpha = 0.18f), iconShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_crown),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.subscription_plan_name, plan),
                style = XpTheme.typography.sectionTitle.copy(fontSize = 17.sp, lineHeight = 22.sp),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            StatusBadge(state.badge)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = colors.divider)
        DetailRow(icon = R.drawable.ic_credit_card, label = stringResource(R.string.billing_cycle), value = plan)
        if (nextBilling != null) {
            HorizontalDivider(color = colors.divider)
            DetailRow(
                icon = R.drawable.ic_calendar,
                label = stringResource(if (state.isCancelled) R.string.billing_access_until else R.string.billing_next_billing_date),
                value = nextBilling,
            )
        }
        billing.trialDaysLeft?.let { days ->
            HorizontalDivider(color = colors.divider)
            DetailRow(icon = null, label = stringResource(R.string.billing_trial_remaining), value = stringResource(R.string.billing_trial_days, days))
        }

        if (state.isCancelled) {
            Spacer(Modifier.height(12.dp))
            Notice(
                icon = R.drawable.ic_x_circle,
                title = stringResource(R.string.billing_cancelled_title),
                body = stringResource(R.string.billing_cancelled_body, nextBilling ?: stringResource(R.string.billing_period_ends)),
            )
        }
        if (billing.isExpiringSoon && state.isTrial) {
            Spacer(Modifier.height(12.dp))
            Notice(
                icon = R.drawable.ic_alert_triangle,
                title = stringResource(R.string.billing_expiring_title),
                body = stringResource(R.string.billing_expiring_body),
            )
        }
    }
}

@Composable
private fun ManagementCard(
    state: BillingUiState,
    billing: Billing,
    onAction: (BillingAction) -> Unit,
    onUpgrade: () -> Unit,
    onGetAccess: () -> Unit,
) {
    val colors = XpTheme.colors
    val nextBilling = billing.nextBillingAt?.billingDate()
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Text(
            text = stringResource(R.string.billing_management),
            style = XpTheme.typography.sectionTitle,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = colors.divider)
        Spacer(Modifier.height(16.dp))

        val text = when {
            state.isRenewing -> stringResource(
                R.string.billing_renewing_text,
                nextBilling ?: stringResource(R.string.billing_next_billing_fallback),
            )
            state.isCancelled -> stringResource(
                R.string.billing_cancelled_text,
                nextBilling ?: stringResource(R.string.billing_period_ends),
            )
            state.isTrial -> stringResource(R.string.billing_trial_text)
            else -> stringResource(R.string.billing_inactive_text)
        }
        Text(
            text = text,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp, lineHeight = 19.sp),
            color = colors.textSecondary,
        )

        when {
            state.isRenewing -> {
                Spacer(Modifier.height(16.dp))
                CancelButton(isCancelling = state.isCancelling, onClick = { onAction(BillingAction.CancelSubscriptionClicked) })
            }
            state.isCancelled -> Unit
            state.isTrial -> {
                Spacer(Modifier.height(16.dp))
                XpActionButton(text = stringResource(R.string.billing_upgrade), onClick = onUpgrade, modifier = Modifier.widthIn(min = 180.dp))
            }
            else -> {
                Spacer(Modifier.height(16.dp))
                XpActionButton(text = stringResource(R.string.subscription_get_access), onClick = onGetAccess, modifier = Modifier.widthIn(min = 140.dp))
            }
        }
    }
}

@Composable
private fun StatusBadge(badge: BillingBadge) {
    val colors = XpTheme.colors
    val (icon, text, color) = when (badge) {
        BillingBadge.CANCELLED -> Triple(R.drawable.ic_x_circle, R.string.billing_badge_cancelled, colors.warning)
        BillingBadge.ACTIVE -> Triple(R.drawable.ic_check_circle, R.string.billing_badge_active, colors.success)
        BillingBadge.TRIAL -> Triple(R.drawable.ic_crown, R.string.billing_badge_trial, colors.trialPurple)
        BillingBadge.INACTIVE -> Triple(R.drawable.ic_alert_triangle, R.string.billing_badge_inactive, colors.error)
    }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.20f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(text),
            style = XpTheme.typography.caption.copy(fontSize = 10.5.sp),
            color = color,
        )
    }
}

@Composable
private fun DetailRow(@DrawableRes icon: Int?, label: String, value: String) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.textLabel, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
            color = colors.textLabel,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
            color = colors.textPrimary,
        )
    }
}

/** The web's amber notices: subscription cancelled, trial expiring soon. */
@Composable
private fun Notice(@DrawableRes icon: Int, title: String, body: String) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.warning.copy(alpha = 0.10f))
            .border(1.dp, colors.warning.copy(alpha = 0.20f), shape)
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp), color = colors.warning)
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = colors.warning.copy(alpha = 0.85f),
            )
        }
    }
}

/** Red outlined "Cancel Subscription", spinning while the request runs. */
@Composable
private fun CancelButton(isCancelling: Boolean, onClick: () -> Unit) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .height(40.dp)
            .alpha(if (isCancelling) 0.6f else 1f)
            .clip(shape)
            .border(1.dp, colors.error.copy(alpha = 0.35f), shape)
            .clickable(enabled = !isCancelling, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCancelling) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = colors.error, strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(if (isCancelling) R.string.billing_cancelling else R.string.billing_cancel),
            style = XpTheme.typography.bodyBold.copy(fontSize = 12.5.sp),
            color = colors.error,
        )
    }
}

@Composable
private fun CancelDialog(accessUntil: String?, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = XpTheme.colors
    val until = accessUntil ?: stringResource(R.string.billing_current_period_ends)
    val body = stringResource(R.string.billing_dialog_body, until)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.error.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_x_circle),
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        title = {
            Text(text = stringResource(R.string.billing_dialog_title), style = XpTheme.typography.dialogTitle, color = colors.textPrimary)
        },
        text = {
            Text(
                text = buildAnnotatedString {
                    append(body)
                    val at = body.indexOf(until)
                    if (at >= 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.textPrimary), at, at + until.length)
                },
                style = XpTheme.typography.dialogBody.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.billing_confirm_cancel),
                    style = XpTheme.typography.buttonSecondary,
                    color = colors.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.billing_keep),
                    style = XpTheme.typography.buttonSecondary,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

private fun previewBilling(status: String, autoRenew: Boolean? = null) = Billing(
    planType = "yearly",
    subscriptionStatus = status,
    isTrial = status == Billing.STATUS_TRIAL,
    expiresAt = null,
    nextBillingAt = Instant.parse("2027-09-26T10:00:00Z"),
    trialDaysLeft = if (status == Billing.STATUS_TRIAL) 3 else null,
    isExpiringSoon = status == Billing.STATUS_TRIAL,
    autoRenew = autoRenew,
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BillingActivePreview() {
    XproKeyTheme(darkTheme = true) {
        BillingScreen(
            state = BillingUiState(isLoading = false, billing = previewBilling(Billing.STATUS_ACTIVE)),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onSectionClick = {},
            onUpgrade = {},
            onGetAccess = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun BillingTrialPreview() {
    XproKeyTheme(darkTheme = false) {
        BillingScreen(
            state = BillingUiState(isLoading = false, billing = previewBilling(Billing.STATUS_TRIAL)),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {},
            onBack = {},
            onSectionClick = {},
            onUpgrade = {},
            onGetAccess = {},
        )
    }
}
