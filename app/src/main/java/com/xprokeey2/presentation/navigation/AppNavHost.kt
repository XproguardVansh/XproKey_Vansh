package com.xprokeey2.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xprokeey2.domain.model.LaunchDestination
import com.xprokeey2.presentation.about.AppInfoScreenRoot
import com.xprokeey2.presentation.about.FaqScreenRoot
import com.xprokeey2.presentation.auth.forgot.ForgotPasswordScreenRoot
import com.xprokeey2.presentation.auth.lock.LockScreenRoot
import com.xprokeey2.presentation.auth.login.LoginScreenRoot
import com.xprokeey2.presentation.auth.reset.ResetPasswordScreenRoot
import com.xprokeey2.presentation.auth.signup.SignupScreenRoot
import com.xprokeey2.presentation.auth.verify.VerifyEmailScreenRoot
import com.xprokeey2.presentation.cards.details.CardDetailsScreenRoot
import com.xprokeey2.presentation.cards.form.CardFormScreenRoot
import com.xprokeey2.presentation.cards.list.CardsScreenRoot
import com.xprokeey2.presentation.dashboard.DashboardScreenRoot
import com.xprokeey2.presentation.legal.LegalDocumentScreen
import com.xprokeey2.presentation.legal.PrivacyPolicy
import com.xprokeey2.presentation.legal.TermsOfService
import com.xprokeey2.presentation.onboarding.AccountTypeScreen
import com.xprokeey2.presentation.onboarding.license.ActivateLicenseScreenRoot
import com.xprokeey2.presentation.onboarding.plan.PlanScreenRoot
import com.xprokeey2.presentation.passwords.details.PasswordDetailsScreenRoot
import com.xprokeey2.presentation.passwords.form.PasswordFormScreenRoot
import com.xprokeey2.presentation.passwords.list.PasswordsScreenRoot
import com.xprokeey2.presentation.payment.RazorpayCheckout
import com.xprokeey2.presentation.profile.ProfileScreenRoot
import com.xprokeey2.presentation.settings.billing.BillingScreenRoot
import com.xprokeey2.presentation.settings.changepassword.ChangePasswordScreenRoot
import com.xprokeey2.presentation.settings.security.SecurityScreenRoot
import com.xprokeey2.presentation.settings.subscription.SubscriptionScreenRoot
import com.xprokeey2.presentation.support.details.SupportTicketScreenRoot
import com.xprokeey2.presentation.support.list.SupportScreenRoot
import com.xprokeey2.presentation.support.newticket.NewTicketScreenRoot
import com.xprokeey2.presentation.tools.exportdata.ExportScreenRoot
import com.xprokeey2.presentation.tools.generator.GeneratorScreenRoot
import com.xprokeey2.presentation.tools.importdata.ImportScreenRoot
import com.xprokeey2.presentation.util.ObserveAsEvents
import com.xprokeey2.presentation.workspace.WorkspaceSection

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val appSession: AppSessionViewModel = hiltViewModel()
    // A screen needs sign-in again: the Lock screen if only the vault key is missing, else Login.
    val signInAgain = { message: String -> appSession.onSignInRequired(message) }
    val context = LocalContext.current
    // Signed out on this device: Razorpay also forgets the saved customer details (Razorpay guide 12).
    val signedOut = { message: String? ->
        RazorpayCheckout.clearUserData(context)
        if (message != null || !navController.isShowing<LoginRoute>()) {
            navController.clearStackAndNavigate(LoginRoute(message = message))
        }
    }

    LifecycleStartEffect(Unit) {
        appSession.onAppForeground()
        onStopOrDispose { appSession.onAppBackground() }
    }

    // Signed in: straight into the app, like the web; the Security timeout may lock or log out instead.
    // Nothing is drawn until this is known (MainActivity waits for it).
    val launchDestination by appSession.launchDestination.collectAsStateWithLifecycle()
    val startDestination: Any = when (launchDestination ?: return) {
        LaunchDestination.LOGIN -> LoginRoute()
        LaunchDestination.APP -> DashboardRoute
        LaunchDestination.LOCK -> LockRoute
    }

    // Collected only once the NavHost below exists, so navigating never comes before its graph.
    ObserveAsEvents(appSession.events) { event ->
        when (event) {
            // A back stack restored after the system closed the app still has to follow the timeout.
            is AppSessionEvent.Launched -> when (event.destination) {
                LaunchDestination.LOGIN -> if (!navController.isShowing<LoginRoute>()) navController.clearStackAndNavigate(LoginRoute())
                LaunchDestination.LOCK -> if (!navController.isShowing<LockRoute>()) navController.clearStackAndNavigate(LockRoute)
                LaunchDestination.APP -> Unit
            }
            // Like the web's API client, but only inside the signed-in app and not onto the same screen.
            AppSessionEvent.OpenCheckout -> if (navController.isInWorkspace() && !navController.isShowing<PlanRoute>()) {
                navController.navigate(PlanRoute) { launchSingleTop = true }
            }
            AppSessionEvent.OpenLicenseActivation ->
                if (navController.isInWorkspace() && !navController.isShowing<ActivateLicenseRoute>()) {
                    navController.navigate(ActivateLicenseRoute) { launchSingleTop = true }
                }
            AppSessionEvent.Locked -> if (!navController.isShowing<LockRoute>()) navController.clearStackAndNavigate(LockRoute)
            AppSessionEvent.SignedOut -> signedOut(null)
            is AppSessionEvent.SignInRequired -> signedOut(event.message)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<LoginRoute> {
            LoginScreenRoot(
                onNavigateToSignup = { navController.navigate(SignupRoute) },
                onNavigateToVerify = { email -> navController.navigate(VerifyEmailRoute(email)) },
                onNavigateToForgotPassword = { email -> navController.navigate(ForgotPasswordRoute(email)) },
                onNavigateToAccountSetup = { navController.clearStackAndNavigate(AccountTypeRoute) },
                onNavigateToDashboard = { navController.clearStackAndNavigate(DashboardRoute) },
            )
        }

        composable<SignupRoute> {
            SignupScreenRoot(
                onNavigateToLogin = navController::backToLogin,
                onNavigateToVerify = { email -> navController.navigate(VerifyEmailRoute(email)) },
                onOpenTerms = { navController.navigate(TermsRoute) },
                onOpenPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
            )
        }

        composable<TermsRoute> {
            LegalDocumentScreen(document = TermsOfService, onBack = { navController.popBackStack() })
        }

        composable<PrivacyPolicyRoute> {
            LegalDocumentScreen(document = PrivacyPolicy, onBack = { navController.popBackStack() })
        }

        composable<VerifyEmailRoute> {
            VerifyEmailScreenRoot(
                onNavigateToLogin = { email -> navController.restartAtLogin(LoginRoute(email)) },
                onBackToSignup = {
                    // Came from Signup -> go back to it; came from a Login redirect -> open Signup.
                    if (!navController.popBackStack<SignupRoute>(inclusive = false)) {
                        navController.navigate(SignupRoute) {
                            popUpTo<VerifyEmailRoute> { inclusive = true }
                        }
                    }
                },
            )
        }

        composable<ForgotPasswordRoute> {
            ForgotPasswordScreenRoot(
                onNavigateToReset = { route -> navController.navigate(route) },
                onBackToLogin = navController::backToLogin,
            )
        }

        composable<ResetPasswordRoute> {
            ResetPasswordScreenRoot(
                onPasswordReset = { email, message ->
                    navController.restartAtLogin(LoginRoute(email = email, message = message))
                },
                onBackToLogin = navController::backToLogin,
            )
        }

        composable<AccountTypeRoute> {
            AccountTypeScreen(
                onPersonalClick = { navController.navigate(PlanRoute) },
                onBusinessClick = { navController.navigate(ActivateLicenseRoute) },
            )
        }

        composable<PlanRoute> {
            PlanScreenRoot(
                onBack = { navController.popBackStack() },
                onFinished = { navController.clearStackAndNavigate(DashboardRoute) },
                // Like the web: nothing to buy, so Manage Subscription instead of this screen. Straight
                // after login (no Dashboard yet) the subscription is simply active: into the app.
                onAlreadyActive = { message ->
                    if (navController.isInWorkspace()) {
                        navController.navigate(BillingRoute) {
                            popUpTo<PlanRoute> { inclusive = true }
                        }
                    } else {
                        navController.clearStackAndNavigate(DashboardRoute)
                    }
                    navController.currentBackStackEntry?.savedStateHandle?.set(RESULT_MESSAGE, message)
                },
                onSignInRequired = signInAgain,
            )
        }

        composable<ActivateLicenseRoute> {
            ActivateLicenseScreenRoot(
                onActivated = { navController.clearStackAndNavigate(DashboardRoute) },
                onSessionExpired = signInAgain,
            )
        }

        composable<DashboardRoute> { entry ->
            val resultMessage by entry.resultMessage()
            DashboardScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onSectionClick = navController::openSection,
                onAddPassword = { navController.navigate(PasswordFormRoute()) },
                onOpenWeakItems = { navController.openSection(WorkspaceSection.PASSWORDS, showWeakItems = true) },
                onViewPassword = { itemId -> navController.navigate(PasswordDetailsRoute(itemId)) },
                onManageCards = { navController.openSection(WorkspaceSection.CARDS) },
                onGeneratePassword = { navController.openSection(WorkspaceSection.GENERATOR) },
                onImportPasswords = { navController.openSection(WorkspaceSection.IMPORT) },
                onSessionExpired = signInAgain,
            )
        }

        composable<PasswordsRoute> { entry ->
            val resultMessage by entry.resultMessage()
            PasswordsScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onSectionClick = navController::openSection,
                onAddPassword = { navController.navigate(PasswordFormRoute()) },
                onViewItem = { itemId -> navController.navigate(PasswordDetailsRoute(itemId)) },
                onSignInRequired = signInAgain,
            )
        }

        composable<PasswordFormRoute> {
            PasswordFormScreenRoot(
                onBack = { navController.popBackStack() },
                onSaved = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }

        composable<PasswordDetailsRoute> { entry ->
            val resultMessage by entry.resultMessage()
            PasswordDetailsScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onBack = { navController.popBackStack() },
                onEdit = {
                    navController.navigate(PasswordFormRoute(itemId = entry.toRoute<PasswordDetailsRoute>().itemId))
                },
                onDeleted = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }

        composable<CardsRoute> { entry ->
            val resultMessage by entry.resultMessage()
            CardsScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onSectionClick = navController::openSection,
                onAddCard = { navController.navigate(CardFormRoute()) },
                onViewCard = { cardId -> navController.navigate(CardDetailsRoute(cardId)) },
                onSignInRequired = signInAgain,
            )
        }

        composable<CardFormRoute> {
            CardFormScreenRoot(
                onBack = { navController.popBackStack() },
                onSaved = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }

        composable<CardDetailsRoute> { entry ->
            val resultMessage by entry.resultMessage()
            CardDetailsScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(CardFormRoute(cardId = entry.toRoute<CardDetailsRoute>().cardId)) },
                onDeleted = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }

        composable<GeneratorRoute> {
            GeneratorScreenRoot(onSectionClick = navController::openSection)
        }

        composable<ExportRoute> {
            ExportScreenRoot(
                onSectionClick = navController::openSection,
                onSignInRequired = signInAgain,
            )
        }

        composable<ImportRoute> {
            ImportScreenRoot(
                onSectionClick = navController::openSection,
                onSignInRequired = signInAgain,
            )
        }

        composable<ChangePasswordRoute> {
            ChangePasswordScreenRoot(
                onSectionClick = navController::openSection,
                // The web goes on to its Profile page, which the app doesn't have: back to the Dashboard.
                onPasswordChanged = { message ->
                    navController.openSection(WorkspaceSection.DASHBOARD)
                    navController.currentBackStackEntry?.savedStateHandle?.set(RESULT_MESSAGE, message)
                },
                onSignInRequired = signInAgain,
            )
        }

        composable<SecurityRoute> {
            SecurityScreenRoot(
                onSectionClick = navController::openSection,
                onCancel = { navController.openSection(WorkspaceSection.DASHBOARD) },
                onSignInRequired = signInAgain,
            )
        }

        composable<SubscriptionRoute> {
            SubscriptionScreenRoot(
                onSectionClick = navController::openSection,
                onManageSubscription = { navController.navigate(BillingRoute) },
                onContinueWithPlan = { navController.navigate(PlanRoute) },
                onGetAccess = { navController.navigate(AccountTypeRoute) },
                onSignInRequired = signInAgain,
            )
        }

        composable<BillingRoute> { entry ->
            val resultMessage by entry.resultMessage()
            BillingScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onBack = { navController.popBackStack() },
                onUpgrade = { navController.navigate(PlanRoute) },
                onGetAccess = { navController.navigate(AccountTypeRoute) },
                onLoadFailed = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }

        composable<ProfileRoute> {
            ProfileScreenRoot(
                onSectionClick = navController::openSection,
                onManageSubscription = { navController.navigate(BillingRoute) },
                onContinueWithPlan = { navController.navigate(PlanRoute) },
                onGetAccess = { navController.navigate(AccountTypeRoute) },
                onSignInRequired = signInAgain,
            )
        }

        composable<LockRoute> {
            LockScreenRoot(
                onUnlocked = { message ->
                    navController.clearStackAndNavigate(DashboardRoute)
                    navController.currentBackStackEntry?.savedStateHandle?.set(RESULT_MESSAGE, message)
                },
                onSignedOut = signedOut,
            )
        }

        composable<AppInfoRoute> {
            AppInfoScreenRoot(onSectionClick = navController::openSection)
        }

        composable<FaqRoute> {
            FaqScreenRoot(
                onSectionClick = navController::openSection,
                onOpenSupport = { navController.openSection(WorkspaceSection.SUPPORT) },
            )
        }

        composable<SupportRoute> { entry ->
            val resultMessage by entry.resultMessage()
            SupportScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onSectionClick = navController::openSection,
                onNewTicket = { navController.navigate(NewTicketRoute) },
                onViewTicket = { ticketId -> navController.navigate(SupportTicketRoute(ticketId)) },
                onSignInRequired = signInAgain,
            )
        }

        composable<NewTicketRoute> {
            NewTicketScreenRoot(
                onBack = { navController.popBackStack() },
                onCreated = { ticketId, message ->
                    // Like the web: straight to the new ticket when the server returns it.
                    if (ticketId == null) {
                        navController.popBackWithMessage(message)
                    } else {
                        navController.navigate(SupportTicketRoute(ticketId)) {
                            popUpTo<NewTicketRoute> { inclusive = true }
                        }
                        navController.currentBackStackEntry?.savedStateHandle?.set(RESULT_MESSAGE, message)
                    }
                },
                onSignInRequired = signInAgain,
            )
        }

        composable<SupportTicketRoute> { entry ->
            val resultMessage by entry.resultMessage()
            SupportTicketScreenRoot(
                resultMessage = resultMessage,
                onResultMessageShown = entry::clearResultMessage,
                onBack = { navController.popBackStack() },
                onLoadFailed = navController::popBackWithMessage,
                onSignInRequired = signInAgain,
            )
        }
    }
}

/** Returns to the Login screen already on the back stack (or opens one). */
private fun NavHostController.backToLogin() {
    if (!popBackStack<LoginRoute>(inclusive = false)) {
        navigate(LoginRoute())
    }
}

/** Flow finished (verified / password reset): clear the auth screens and start fresh on Login. */
private fun NavHostController.restartAtLogin(route: LoginRoute) {
    navigate(route) {
        popUpTo<LoginRoute> { inclusive = true }
    }
}

/** Leaves the current flow for good (signed in, set up, session expired): Back can't return to it. */
private fun <T : Any> NavHostController.clearStackAndNavigate(route: T) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/**
 * Drawer / dashboard shortcuts. Dashboard stays at the bottom of the stack, so Back from a section
 * returns to it. [showWeakItems] opens Passwords on the "Weak Items" filter.
 */
private fun NavHostController.openSection(section: WorkspaceSection, showWeakItems: Boolean = false) {
    when (section) {
        WorkspaceSection.DASHBOARD -> if (!popBackStack<DashboardRoute>(inclusive = false)) {
            clearStackAndNavigate(DashboardRoute)
        }
        WorkspaceSection.PASSWORDS -> navigate(PasswordsRoute(showWeakItems)) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.CARDS -> navigate(CardsRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.GENERATOR -> navigate(GeneratorRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.EXPORT -> navigate(ExportRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.IMPORT -> navigate(ImportRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.SUPPORT -> navigate(SupportRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.CHANGE_PASSWORD -> navigate(ChangePasswordRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.SECURITY -> navigate(SecurityRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.SUBSCRIPTION -> navigate(SubscriptionRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.PROFILE -> navigate(ProfileRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.APP_INFO -> navigate(AppInfoRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        WorkspaceSection.FAQ -> navigate(FaqRoute) {
            popUpTo<DashboardRoute>()
            launchSingleTop = true
        }
        // Not built yet (disabled in the drawer).
        else -> Unit
    }
}

/** Signed in and inside the app: the Dashboard is at the bottom of every workspace back stack. */
private fun NavHostController.isInWorkspace(): Boolean =
    runCatching { getBackStackEntry<DashboardRoute>() }.isSuccess

private inline fun <reified T : Any> NavHostController.isShowing(): Boolean =
    currentDestination?.hasRoute<T>() == true

private const val RESULT_MESSAGE = "result_message"

/** Goes back and hands [message] to the previous screen, which shows it once (e.g. "Card saved"). */
private fun NavHostController.popBackWithMessage(message: String) {
    previousBackStackEntry?.savedStateHandle?.set(RESULT_MESSAGE, message)
    popBackStack()
}

@Composable
private fun NavBackStackEntry.resultMessage() =
    savedStateHandle.getStateFlow<String?>(RESULT_MESSAGE, null).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearResultMessage() {
    savedStateHandle[RESULT_MESSAGE] = null
}
