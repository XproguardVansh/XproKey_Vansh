package com.xprokeey2.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xprokeey2.presentation.auth.forgot.ForgotPasswordScreenRoot
import com.xprokeey2.presentation.auth.login.LoginScreenRoot
import com.xprokeey2.presentation.auth.reset.ResetPasswordScreenRoot
import com.xprokeey2.presentation.auth.signup.SignupScreenRoot
import com.xprokeey2.presentation.auth.verify.VerifyEmailScreenRoot
import com.xprokeey2.presentation.legal.LegalDocumentScreen
import com.xprokeey2.presentation.legal.PrivacyPolicy
import com.xprokeey2.presentation.legal.TermsOfService

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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
