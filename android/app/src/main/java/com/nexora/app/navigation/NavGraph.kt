package com.nexora.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nexora.app.NexoraApp
import com.nexora.app.ui.screens.device.DeviceDetailScreen
import com.nexora.app.ui.screens.device.DeviceViewModel
import com.nexora.app.ui.screens.device.DeviceViewModelFactory
import com.nexora.app.ui.screens.energy.EnergyScreen
import com.nexora.app.ui.screens.energy.EnergyViewModel
import com.nexora.app.ui.screens.energy.EnergyViewModelFactory
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

import com.nexora.app.ui.screens.ai.AiInsightsScreen
import com.nexora.app.ui.screens.ai.AiViewModel
import com.nexora.app.ui.screens.ai.AiViewModelFactory
import com.nexora.app.ui.screens.log.ActivityLogScreen
import com.nexora.app.ui.screens.log.DecisionLogScreen
import com.nexora.app.ui.screens.log.LogViewModel
import com.nexora.app.ui.screens.log.LogViewModelFactory

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val HOME = "home"
    const val ROOM_DETAIL = "room_detail/{homeId}/{roomId}"
    const val DEVICE_DETAIL = "device_detail/{deviceId}"
    const val ENERGY = "energy/{homeId}"
    const val AI_INSIGHTS = "ai_insights/{homeId}"
    const val ACTIVITY_LOG = "activity_log/{homeId}"
    const val DECISION_LOG = "decision_log/{homeId}"

    fun buildRoomDetailRoute(homeId: Int, roomId: Int): String {
        return "room_detail/$homeId/$roomId"
    }

    fun buildDeviceDetailRoute(deviceId: String): String {
        return "device_detail/$deviceId"
    }

    fun buildEnergyRoute(homeId: Int): String {
        return "energy/$homeId"
    }

    fun buildAiInsightsRoute(homeId: Int): String {
        return "ai_insights/$homeId"
    }

    fun buildActivityLogRoute(homeId: Int): String {
        return "activity_log/$homeId"
    }

    fun buildDecisionLogRoute(homeId: Int): String {
        return "decision_log/$homeId"
    }
}

@Composable
fun NexoraNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val app = context.applicationContext as NexoraApp

    val isSessionExpired by app.sessionRepository.isSessionExpired.collectAsState()

    LaunchedEffect(isSessionExpired) {
        if (isSessionExpired) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            app.sessionRepository.resetSessionExpired()
        }
    }
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
                },
                onNavigateToDeviceDetail = { deviceId ->
                    navController.navigate(Routes.buildDeviceDetailRoute(deviceId))
                },
                onNavigateToEnergy = { homeId ->
                    navController.navigate(Routes.buildEnergyRoute(homeId))
                },
                onNavigateToAiInsights = { homeId ->
                    navController.navigate(Routes.buildAiInsightsRoute(homeId))
                },
                onNavigateToActivityLog = { homeId ->
                    navController.navigate(Routes.buildActivityLogRoute(homeId))
                },
                onNavigateToDecisionLog = { homeId ->
                    navController.navigate(Routes.buildDecisionLogRoute(homeId))
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
        composable(
            route = Routes.DEVICE_DETAIL,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            val deviceViewModel: DeviceViewModel = viewModel(
                factory = DeviceViewModelFactory(app.deviceRepository)
            )

            DeviceDetailScreen(
                deviceId = deviceId,
                viewModel = deviceViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.ENERGY,
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getInt("homeId") ?: 0
            val energyViewModel: EnergyViewModel = viewModel(
                factory = EnergyViewModelFactory(homeId, app.energyRepository)
            )
            EnergyScreen(
                viewModel = energyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.AI_INSIGHTS,
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getInt("homeId") ?: 0
            val aiViewModel: AiViewModel = viewModel(
                factory = AiViewModelFactory(homeId, app.aiRepository)
            )
            AiInsightsScreen(
                viewModel = aiViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.ACTIVITY_LOG,
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getInt("homeId") ?: 0
            val logViewModel: LogViewModel = viewModel(
                factory = LogViewModelFactory(homeId, app.logRepository)
            )
            ActivityLogScreen(
                viewModel = logViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = Routes.DECISION_LOG,
            arguments = listOf(
                navArgument("homeId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val homeId = backStackEntry.arguments?.getInt("homeId") ?: 0
            val logViewModel: LogViewModel = viewModel(
                factory = LogViewModelFactory(homeId, app.logRepository)
            )
            DecisionLogScreen(
                viewModel = logViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
