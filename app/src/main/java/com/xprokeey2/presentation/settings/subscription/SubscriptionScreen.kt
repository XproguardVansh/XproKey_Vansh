package com.xprokeey2.presentation.settings.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.SubscriptionOverview
import com.xprokeey2.presentation.settings.billingDate
import com.xprokeey2.presentation.settings.capitalizeFirst
import com.xprokeey2.presentation.settings.capitalizeWords
import com.xprokeey2.presentation.theme.XpTheme
import com.xprokeey2.presentation.theme.XproKeyTheme
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.LoadingBlock
import com.xprokeey2.presentation.workspace.UserBadge
import com.xprokeey2.presentation.workspace.WorkspacePanel
import com.xprokeey2.presentation.workspace.WorkspaceScaffold
import com.xprokeey2.presentation.workspace.WorkspaceSection
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SubscriptionScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onManageSubscription: () -> Unit,
    onContinueWithPlan: () -> Unit,
    onGetAccess: () -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SubscriptionEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is SubscriptionEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SubscriptionScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSectionClick = onSectionClick,
        onNextStep = { step ->
            when (step) {
                SubscriptionNextStep.MANAGE -> onManageSubscription()
                SubscriptionNextStep.CONTINUE_WITH_PLAN -> onContinueWithPlan()
                SubscriptionNextStep.GET_ACCESS -> onGetAccess()
            }
        },
    )
}

/** Settings > Subscription, laid out like the web page. */
@Composable
fun SubscriptionScreen(
    state: SubscriptionUiState,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onNextStep: (SubscriptionNextStep) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.SUBSCRIPTION,
        onSectionClick = onSectionClick,
        snackbarHostState = snackbarHostState,
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
                text = stringResource(R.string.subscription_subtitle),
                style = XpTheme.typography.body.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(24.dp))
            when (val overview = state.overview) {
                is SubscriptionOverview.Personal -> PersonalPlanCard(overview, onNextStep)
                is SubscriptionOverview.Business -> BusinessLicenseCard(overview)
                null -> if (state.isLoading) LoadingBlock()
            }
        }
    }
}

@Composable
private fun PersonalPlanCard(plan: SubscriptionOverview.Personal, onNextStep: (SubscriptionNextStep) -> Unit) {
    val colors = XpTheme.colors
    val status = plan.status?.lowercase()
    val badgeColor = when (status) {
        "active" -> colors.success
        "trial" -> colors.warning
        else -> colors.error
    }
    val planLabel = plan.planType?.capitalizeFirst() ?: stringResource(R.string.subscription_plan_fallback)
    val nextStep = SubscriptionNextStep.of(plan.status)

    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        CardHeader(
            title = if (plan.planType != null) {
                stringResource(R.string.subscription_plan_name, planLabel)
            } else {
                stringResource(R.string.subscription_current_plan)
            },
            subtitle = stringResource(R.string.subscription_active_subscription),
            badge = plan.status?.capitalizeWords() ?: stringResource(R.string.subscription_status_unknown),
            badgeColor = badgeColor,
        )

        val tiles = buildList<@Composable (Modifier) -> Unit> {
            plan.daysLeft?.let { days ->
                add { modifier ->
                    InfoTile(
                        label = stringResource(R.string.subscription_days_remaining),
                        value = days.toString(),
                        valueStyle = XpTheme.typography.statValue.copy(fontSize = 30.sp, lineHeight = 36.sp),
                        caption = stringResource(R.string.subscription_days_left),
                        modifier = modifier,
                    )
                }
            }
            plan.nextBillingAt?.let { date ->
                add { modifier ->
                    InfoTile(
                        label = stringResource(R.string.subscription_next_billing),
                        value = date.billingDate(),
                        valueStyle = XpTheme.typography.statValue.copy(fontSize = 20.sp, lineHeight = 26.sp),
                        caption = stringResource(R.string.subscription_auto_renews),
                        modifier = modifier,
                    )
                }
            }
        }
        if (tiles.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            TileGrid(tiles)
        }

        Spacer(Modifier.height(24.dp))
        NextStepButton(
            text = when (nextStep) {
                SubscriptionNextStep.MANAGE -> stringResource(R.string.subscription_manage)
                SubscriptionNextStep.CONTINUE_WITH_PLAN -> stringResource(R.string.subscription_continue_with, planLabel)
                SubscriptionNextStep.GET_ACCESS -> stringResource(R.string.subscription_get_access)
            },
            onClick = { onNextStep(nextStep) },
        )
    }
}

@Composable
private fun BusinessLicenseCard(license: SubscriptionOverview.Business) {
    val colors = XpTheme.colors
    val status = license.licenseStatus.orEmpty().capitalizeWords()
    val badgeColor = if (license.licenseStatus?.lowercase() == "active") colors.success else colors.warning
    val valueStyle = XpTheme.typography.statValue.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)

    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        CardHeader(
            title = stringResource(R.string.subscription_business_plan),
            subtitle = stringResource(R.string.subscription_business_subtitle),
            badge = status,
            badgeColor = badgeColor,
        )

        val tiles = buildList<@Composable (Modifier) -> Unit> {
            license.licenseExpiresAt?.let { date ->
                add { modifier ->
                    InfoTile(stringResource(R.string.subscription_license_expiry), date.billingDate(), valueStyle, modifier = modifier)
                }
            }
            add { modifier -> InfoTile(stringResource(R.string.subscription_status), status, valueStyle, modifier = modifier) }
            add { modifier ->
                InfoTile(stringResource(R.string.subscription_plan_type), stringResource(R.string.subscription_business), valueStyle, modifier = modifier)
            }
        }
        Spacer(Modifier.height(24.dp))
        TileGrid(tiles)
    }
}

/** Shield tile, plan name and status badge. */
@Composable
private fun CardHeader(title: String, subtitle: String, badge: String, badgeColor: Color) {
    val colors = XpTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        val iconShape = RoundedCornerShape(12.dp)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(iconShape)
                .background(badgeColor.copy(alpha = 0.10f))
                .border(1.dp, badgeColor.copy(alpha = 0.20f), iconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = XpTheme.typography.sectionTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = XpTheme.typography.body.copy(fontSize = 12.5.sp),
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusBadge(text = badge, color = badgeColor)
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        style = XpTheme.typography.bodyBold.copy(fontSize = 11.5.sp),
        color = color,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.20f), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/** Side by side when there's room, like the web's grid; stacked on phones. */
@Composable
private fun TileGrid(tiles: List<@Composable (Modifier) -> Unit>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 520.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.forEach { tile -> tile(Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.forEach { tile -> tile(Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
private fun InfoTile(
    label: String,
    value: String,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.fieldBackground)
            .border(1.dp, colors.divider, shape)
            .padding(16.dp),
    ) {
        Text(text = label, style = XpTheme.typography.body.copy(fontSize = 11.5.sp), color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Text(text = value, style = valueStyle, color = colors.textPrimary)
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = caption, style = XpTheme.typography.body.copy(fontSize = 11.5.sp), color = colors.textSecondary)
        }
    }
}

/** "Manage Subscription ›" / "Continue with … Plan ›" / "Get Access ›" */
@Composable
private fun NextStepButton(text: String, onClick: () -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = XpTheme.typography.button.copy(fontSize = 13.sp),
            color = colors.onPrimary,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PersonalTrialPreview() {
    XproKeyTheme(darkTheme = true) {
        SubscriptionScreen(
            state = SubscriptionUiState(
                user = UserBadge.from(name = "Vansh Goel", email = "goelv2610@gmail.com"),
                isLoading = false,
                overview = SubscriptionOverview.Personal(status = "trial", daysLeft = 3, nextBillingAt = null, planType = "yearly"),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onNextStep = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun BusinessLicensePreview() {
    XproKeyTheme(darkTheme = false) {
        SubscriptionScreen(
            state = SubscriptionUiState(
                isLoading = false,
                overview = SubscriptionOverview.Business(
                    licenseStatus = "active",
                    licenseExpiresAt = Instant.parse("2027-09-26T10:00:00Z"),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onNextStep = {},
        )
    }
}
