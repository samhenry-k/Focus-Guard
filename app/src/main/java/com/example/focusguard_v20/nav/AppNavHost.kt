package com.example.focusguard_v20.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.focusguard_v20.auth.AuthState
import com.example.focusguard_v20.auth.AuthViewModel
import com.example.focusguard_v20.ui.screens.LoginScreen
import com.example.focusguard_v20.ui.screens.SignUpScreen
import com.example.focusguard_v20.ui.screens.WelcomeScreen
import com.example.focusguard_v20.ui.shell.AppShell

private object Routes {
    const val Login = "login"
    const val SignUp = "signup"
    const val Welcome = "welcome"
    const val App = "app"
}

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val showWelcomeAfterSignup by authViewModel.showWelcomeAfterSignup.collectAsStateWithLifecycle()

    LaunchedEffect(authState, showWelcomeAfterSignup) {
        when (authState) {
            is AuthState.LoggedIn ->
                if (showWelcomeAfterSignup) {
                    navController.navigate(Routes.Welcome) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Routes.App) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }

            is AuthState.LoggedOut ->
                navController.navigate(Routes.Login) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }

            AuthState.Loading -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Login,
    ) {
        composable(Routes.Login) {
            LoginScreen(
                onLogin = { email, password, onResult ->
                    authViewModel.login(email, password) { result ->
                        onResult(result)
                    }
                },
                onGoToSignUp = { navController.navigate(Routes.SignUp) },
            )
        }

        composable(Routes.SignUp) {
            SignUpScreen(
                onSignUp = { email, password, onResult ->
                    authViewModel.signUp(email, password) { result ->
                        onResult(result)
                    }
                },
                onGoToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.Welcome) {
            WelcomeScreen(
                email = (authState as? AuthState.LoggedIn)?.email ?: authViewModel.currentEmailOrNull().orEmpty(),
                onContinue = {
                    authViewModel.consumeWelcomeAfterSignup()
                    navController.navigate(Routes.App) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.App) {
            AppShell(
                onLogout = { authViewModel.logout() },
            )
        }
    }
}

