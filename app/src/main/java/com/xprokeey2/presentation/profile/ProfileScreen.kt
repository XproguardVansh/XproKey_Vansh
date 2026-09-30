package com.xprokeey2.presentation.profile

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xprokeey2.R
import com.xprokeey2.domain.model.AccountProfile
import com.xprokeey2.domain.model.Billing
import com.xprokeey2.domain.model.ProfileOverview
import com.xprokeey2.presentation.settings.billingDate
import com.xprokeey2.presentation.settings.capitalizeFirst
import com.xprokeey2.presentation.settings.capitalizeWords
import com.xprokeey2.presentation.settings.subscription.SubscriptionNextStep
import com.xprokeey2.presentation.settings.trialPurple
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
fun ProfileScreenRoot(
    onSectionClick: (WorkspaceSection) -> Unit,
    onManageSubscription: () -> Unit,
    onContinueWithPlan: () -> Unit,
    onGetAccess: () -> Unit,
    onSignInRequired: (message: String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ProfileEvent.ShowMessage -> scope.launch { snackbarHostState.showSnackbar(event.message.asString(context)) }
            is ProfileEvent.SignInRequired -> onSignInRequired(event.message.asString(context))
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ProfileScreen(
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

/** Profile, laid out like the web page: its phone layout here, its desktop layout on wide screens. */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onSectionClick: (WorkspaceSection) -> Unit,
    onNextStep: (SubscriptionNextStep) -> Unit,
) {
    val colors = XpTheme.colors
    WorkspaceScaffold(
        user = state.user,
        currentSection = WorkspaceSection.PROFILE,
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
                text = stringResource(R.string.nav_profile),
                style = XpTheme.typography.pageTitle,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(24.dp))

            val overview = state.overview
            if (overview == null) {
                if (state.isLoading) LoadingBlock()
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 600.dp
                    Column {
                        AccountPanel(profile = overview.profile, isWide = isWide)
                        Spacer(Modifier.height(20.dp))
                        if (overview.profile.isBusiness) {
                            LicensePanel(profile = overview.profile)
                        } else {
                            PlanPanel(overview = overview, isWide = isWide, onNextStep = onNextStep)
                        }
                    }
                }
            }
        }
    }
}

/** Initials, name, and "email • Joined …"; centred on phones like the web's small-screen layout. */
@Composable
private fun AccountPanel(profile: AccountProfile, isWide: Boolean) {
    val colors = XpTheme.colors
    val fullName = profile.fullName()
    val initials = profileInitials(fullName, profile.email)
    val details = stringResource(R.string.profile_joined, profile.email, joinedDate(profile.joinedAt))

    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        val avatar = @Composable {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = XpTheme.typography.bodyBold.copy(fontSize = 20.sp, lineHeight = 24.sp),
                    color = colors.onPrimary,
                )
            }
        }
        val name = @Composable { textAlign: TextAlign ->
            Text(
                text = fullName,
                style = XpTheme.typography.sectionTitle.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = colors.textPrimary,
                textAlign = textAlign,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = details,
                style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = colors.textSecondary,
                textAlign = textAlign,
            )
        }
        if (isWide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                avatar()
                Spacer(Modifier.width(18.dp))
                Column { name(TextAlign.Start) }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                avatar()
                Spacer(Modifier.height(16.dp))
                name(TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LicensePanel(profile: AccountProfile) {
    val colors = XpTheme.colors
    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        CardHeader(icon = R.drawable.ic_building_2, title = stringResource(R.string.profile_business_license))
        Spacer(Modifier.height(20.dp))
        StatusBanner(ProfileBanner.BUSINESS, text = stringResource(R.string.profile_banner_business))
        Spacer(Modifier.height(8.dp))
        DetailRow(label = stringResource(R.string.profile_organization)) {
            DetailValue(profile.organizationName ?: stringResource(R.string.profile_no_value))
        }
        HorizontalDivider(color = colors.divider)
        DetailRow(label = stringResource(R.string.profile_license_status)) {
            Badge(
                text = (profile.licenseStatus ?: stringResource(R.string.profile_license_inactive)).capitalizeWords(),
                color = colors.primary,
            )
        }
        HorizontalDivider(color = colors.divider)
        DetailRow(label = stringResource(R.string.profile_valid_till)) {
            DetailValue(profile.licenseExpiresAt?.let { validTillDate(it) } ?: stringResource(R.string.profile_no_value))
        }
    }
}

@Composable
private fun PlanPanel(overview: ProfileOverview, isWide: Boolean, onNextStep: (SubscriptionNextStep) -> Unit) {
    val colors = XpTheme.colors
    val billing = overview.billing
    val banner = ProfileBanner.of(overview)
    val plan = billing?.planType?.capitalizeFirst() ?: stringResource(R.string.billing_not_purchased)
    val nextBilling = billing?.nextBillingAt?.billingDate()
    val nextStep = SubscriptionNextStep.of(billing?.subscriptionStatus)

    WorkspacePanel(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeader(
                icon = R.drawable.ic_crown,
                title = stringResource(R.string.subscription_plan_name, plan),
                modifier = Modifier.weight(1f),
            )
            if (banner == ProfileBanner.CANCELLED) {
                Spacer(Modifier.width(8.dp))
                Badge(text = stringResource(R.string.billing_badge_cancelled), color = colors.warning)
            }
        }
        Spacer(Modifier.height(20.dp))
        StatusBanner(
            banner = banner,
            text = when (banner) {
                ProfileBanner.CANCELLED -> stringResource(
                    R.string.profile_banner_cancelled,
                    nextBilling ?: stringResource(R.string.billing_period_ends),
                )
                ProfileBanner.EXPIRING_SOON -> stringResource(R.string.profile_banner_expiring)
                ProfileBanner.ACTIVE -> stringResource(R.string.profile_banner_active)
                ProfileBanner.TRIAL -> stringResource(R.string.profile_banner_trial, billing?.trialDaysLeft?.toString().orEmpty())
                ProfileBanner.NO_SUBSCRIPTION, ProfileBanner.BUSINESS -> stringResource(R.string.profile_banner_none)
            },
        )
        Spacer(Modifier.height(8.dp))
        DetailRow(label = stringResource(R.string.billing_cycle)) { DetailValue(plan) }
        if (nextBilling != null) {
            HorizontalDivider(color = colors.divider)
            DetailRow(
                label = stringResource(
                    if (banner == ProfileBanner.CANCELLED) R.string.billing_access_until else R.string.billing_next_billing_date
                ),
            ) { DetailValue(nextBilling) }
        }
        billing?.trialDaysLeft?.let { days ->
            HorizontalDivider(color = colors.divider)
            DetailRow(label = stringResource(R.string.billing_trial_remaining)) {
                DetailValue(stringResource(R.string.billing_trial_days, days))
            }
        }

        Spacer(Modifier.height(20.dp))
        NextStepButton(
            text = when (nextStep) {
                SubscriptionNextStep.MANAGE -> stringResource(R.string.subscription_manage)
                SubscriptionNextStep.CONTINUE_WITH_PLAN -> stringResource(R.string.subscription_continue_with, plan)
                SubscriptionNextStep.GET_ACCESS -> stringResource(R.string.subscription_get_access)
            },
            onClick = { onNextStep(nextStep) },
            modifier = if (isWide) Modifier.align(Alignment.End) else Modifier.fillMaxWidth(),
        )
    }
}

/** Icon tile and the card's name in capitals ("YEARLY PLAN", "BUSINESS LICENSE"). */
@Composable
private fun CardHeader(@DrawableRes icon: Int, title: String, modifier: Modifier = Modifier) {
    val colors = XpTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f))
                .border(1.dp, colors.primary.copy(alpha = 0.15f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = XpTheme.typography.bodyBold.copy(fontSize = 13.sp, letterSpacing = 0.8.sp),
            color = colors.textPrimary,
        )
    }
}

/** The web's coloured status line: blue business, amber cancelled or expiring, green active, purple trial, red none. */
@Composable
private fun StatusBanner(banner: ProfileBanner, text: String) {
    val colors = XpTheme.colors
    val (icon, color) = when (banner) {
        ProfileBanner.BUSINESS -> R.drawable.ic_building_2 to colors.primary
        ProfileBanner.CANCELLED -> R.drawable.ic_x_circle to colors.warning
        ProfileBanner.EXPIRING_SOON -> R.drawable.ic_alert_triangle to colors.warning
        ProfileBanner.ACTIVE -> R.drawable.ic_check_circle to colors.success
        ProfileBanner.TRIAL -> R.drawable.ic_crown to colors.trialPurple
        ProfileBanner.NO_SUBSCRIPTION -> R.drawable.ic_alert_triangle to colors.error
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = if (colors.isDark) 0.08f else 0.10f))
            .border(1.dp, color.copy(alpha = if (colors.isDark) 0.30f else 0.25f), shape)
            .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = XpTheme.typography.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = color,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: @Composable () -> Unit) {
    val colors = XpTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = XpTheme.typography.body.copy(fontSize = 12.sp),
            color = colors.textLabel,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        value()
    }
}

@Composable
private fun DetailValue(text: String) {
    Text(
        text = text,
        style = XpTheme.typography.bodyBold.copy(fontSize = 12.sp),
        color = XpTheme.colors.textPrimary,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun Badge(text: String, color: Color) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = text,
        style = XpTheme.typography.caption.copy(fontSize = 10.sp),
        color = color,
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.20f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** "Manage Subscription ›" / "Continue with … Plan ›" / "Get Access ›" */
@Composable
private fun NextStepButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = XpTheme.colors
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primary)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp),
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

private val PreviewBadge = UserBadge.from(name = "", email = "goelvansh770@gmail.com")

private fun previewProfile(accountType: String) = AccountProfile(
    email = "goelvansh770@gmail.com",
    name = "",
    accountType = accountType,
    joinedAt = Instant.parse("2026-09-25T10:00:00Z"),
    organizationName = "kk",
    licenseStatus = "active",
    licenseExpiresAt = Instant.parse("2027-09-26T10:00:00Z"),
)

@Preview(name = "Cancelled plan", showBackground = true, heightDp = 900)
@Composable
private fun ProfileCancelledPreview() {
    XproKeyTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileUiState(
                user = PreviewBadge,
                isLoading = false,
                overview = ProfileOverview(
                    profile = previewProfile("personal"),
                    billing = Billing(
                        planType = "yearly",
                        subscriptionStatus = Billing.STATUS_ACTIVE,
                        isTrial = false,
                        expiresAt = null,
                        nextBillingAt = Instant.parse("2027-09-30T10:00:00Z"),
                        trialDaysLeft = null,
                        isExpiringSoon = false,
                        autoRenew = false,
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onNextStep = {},
        )
    }
}

@Preview(name = "Trial, dark", showBackground = true, heightDp = 900)
@Composable
private fun ProfileTrialPreview() {
    XproKeyTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(
                user = PreviewBadge,
                isLoading = false,
                overview = ProfileOverview(
                    profile = previewProfile("personal"),
                    billing = Billing(
                        planType = "quarterly",
                        subscriptionStatus = Billing.STATUS_TRIAL,
                        isTrial = true,
                        expiresAt = null,
                        nextBillingAt = null,
                        trialDaysLeft = 6,
                        isExpiringSoon = false,
                        autoRenew = null,
                    ),
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onNextStep = {},
        )
    }
}

@Preview(name = "Business, wide", showBackground = true, heightDp = 700, widthDp = 800)
@Composable
private fun ProfileBusinessPreview() {
    XproKeyTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileUiState(
                user = PreviewBadge,
                isLoading = false,
                overview = ProfileOverview(profile = previewProfile("business"), billing = null),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSectionClick = {},
            onNextStep = {},
        )
    }
}
