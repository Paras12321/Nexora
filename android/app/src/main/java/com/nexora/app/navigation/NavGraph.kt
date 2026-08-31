package com.nexora.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nexora.app.NexoraApp
import com.nexora.app.ui.screens.forgot_password.ForgotPasswordScreen
import com.nexora.app.ui.screens.login.AuthViewModel
import com.nexora.app.ui.screens.login.AuthViewModelFactory
import com.nexora.app.ui.screens.login.LoginScreen
import com.nexora.app.ui.screens.register.RegisterScreen
import com.nexora.app.ui.screens.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
}

@Composable
fun NexoraNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as NexoraApp
    val authRepository = app.authRepository

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onSplashFinished = { isLoggedIn ->
                val destination = if (isLoggedIn) Routes.HOME else Routes.LOGIN
                navController.navigate(destination) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(authRepository)
            )
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.HOME) {
            // Placeholder for Home screen (Task A2)
            androidx.compose.material3.Text("Home Screen Placeholder")
        }
    }
}
