package com.xprokeey2.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.xprokeey2.presentation.about.AppInfoScreenRoot
import com.xprokeey2.presentation.about.FaqScreenRoot
import com.xprokeey2.presentation.auth.forgot.ForgotPasswordScreenRoot
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
import com.xprokeey2.presentation.onboarding.plan.PlanScreen
import com.xprokeey2.presentation.passwords.details.PasswordDetailsScreenRoot
import com.xprokeey2.presentation.passwords.form.PasswordFormScreenRoot
import com.xprokeey2.presentation.passwords.list.PasswordsScreenRoot
import com.xprokeey2.presentation.support.details.SupportTicketScreenRoot
import com.xprokeey2.presentation.support.list.SupportScreenRoot
import com.xprokeey2.presentation.support.newticket.NewTicketScreenRoot
import com.xprokeey2.presentation.tools.exportdata.ExportScreenRoot
import com.xprokeey2.presentation.tools.generator.GeneratorScreenRoot
import com.xprokeey2.presentation.tools.importdata.ImportScreenRoot
import com.xprokeey2.presentation.workspace.WorkspaceSection

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Session over or vault locked: start again at Login, showing why.
    val signInAgain = { message: String -> navController.clearStackAndNavigate(LoginRoute(message = message)) }

    NavHost(
        navController = navController,
        startDestination = LoginRoute(),
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
            PlanScreen(onBack = { navController.popBackStack() })
        }

        composable<ActivateLicenseRoute> {
            ActivateLicenseScreenRoot(
                onActivated = { navController.clearStackAndNavigate(DashboardRoute) },
                onSessionExpired = signInAgain,
            )
        }

        composable<DashboardRoute> {
            DashboardScreenRoot(
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
