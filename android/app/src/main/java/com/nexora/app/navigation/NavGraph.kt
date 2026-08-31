package com.nexora.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexora.app.NexoraApp
import com.nexora.app.ui.screens.forgot_password.ForgotPasswordScreen
import com.nexora.app.ui.screens.home.HomeScreen
import com.nexora.app.ui.screens.home.HomeViewModel
import com.nexora.app.ui.screens.home.HomeViewModelFactory
import com.nexora.app.ui.screens.login.AuthViewModel
import com.nexora.app.ui.screens.login.AuthViewModelFactory
import com.nexora.app.ui.screens.login.LoginScreen
import com.nexora.app.ui.screens.register.RegisterScreen
import com.nexora.app.ui.screens.room.RoomDetailScreen
import com.nexora.app.ui.screens.room.RoomDetailViewModel
import com.nexora.app.ui.screens.room.RoomDetailViewModelFactory
import com.nexora.app.ui.screens.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val ROOM_DETAIL = "room_detail/{homeId}/{roomId}"

    fun buildRoomDetailRoute(homeId: Int, roomId: Int): String {
        return "room_detail/$homeId/$roomId"
    }
}

@Composable
fun NexoraNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as NexoraApp

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
                factory = AuthViewModelFactory(app.authRepository)
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
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(app.homeRepository, app.roomRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToRoomDetail = { homeId, roomId ->
                    navController.navigate(Routes.buildRoomDetailRoute(homeId, roomId))
                }
            )
        }
        composable(
            route = Routes.ROOM_DETAIL,
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType },
                navArgument("roomId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getInt("homeId") ?: 0
            val roomId = backStackEntry.arguments?.getInt("roomId") ?: 0

            val roomDetailViewModel: RoomDetailViewModel = viewModel(
                factory = RoomDetailViewModelFactory(homeId, roomId, app.roomRepository, app.homeRepository)
            )

            RoomDetailScreen(
                viewModel = roomDetailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
