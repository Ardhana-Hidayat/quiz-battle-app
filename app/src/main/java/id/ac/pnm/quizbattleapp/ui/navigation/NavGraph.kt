package id.ac.pnm.quizbattleapp.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import id.ac.pnm.quizbattleapp.data.model.GameResult
import id.ac.pnm.quizbattleapp.feature.auth.*
import id.ac.pnm.quizbattleapp.feature.game.*
import id.ac.pnm.quizbattleapp.feature.history.HistoryScreen
import id.ac.pnm.quizbattleapp.feature.home.HomeScreen
import id.ac.pnm.quizbattleapp.feature.leaderboard.LeaderboardScreen
import id.ac.pnm.quizbattleapp.feature.lobby.LobbyScreen
import id.ac.pnm.quizbattleapp.feature.result.ResultScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {

    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Auth.route

    // State sementara untuk pass GameResult ke ResultScreen
    // (Jetpack Navigation belum support passing object kompleks langsung)
    var pendingResult by remember { mutableStateOf<GameResult?>(null) }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.Auth.route) {
            AuthScreen(onAuthSuccess = {
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.Auth.route) { inclusive = true }
                }
            })
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onOnlineBattle = { navController.navigate(Routes.Lobby.route) },
                onSoloTraining = { navController.navigate(Routes.Solo.route) },
                onLeaderboard  = { navController.navigate(Routes.Leaderboard.route) },
                onLogout = {
                    navController.navigate(Routes.Auth.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Solo.route) {
            SoloScreen(
                onNavigateBack = { navController.popBackStack() },
                onGameFinished = { result ->
                    pendingResult = result
                    navController.navigate(Routes.Result.route) {
                        popUpTo(Routes.Solo.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Lobby.route) {
            LobbyScreen(
                onGameStart = { roomId ->
                    navController.navigate("${Routes.Game.route}/$roomId") {
                        popUpTo(Routes.Lobby.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = "${Routes.Game.route}/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) {
            GameScreen(
                roomId = it.arguments?.getString("roomId") ?: "",
                onGameFinished = { result ->
                    pendingResult = result
                    navController.navigate(Routes.Result.route) {
                        popUpTo(Routes.Lobby.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Result.route) {
            val result = pendingResult
            if (result == null) {
                // Guard: jika dibuka tanpa result, kembali ke home
                LaunchedEffect(Unit) { navController.navigate(Routes.Home.route) }
            } else {
                ResultScreen(
                    result      = result,
                    onPlayAgain = {
                        pendingResult = null
                        val destination = if (result.mode == "solo") Routes.Solo.route
                                          else Routes.Lobby.route
                        navController.navigate(destination) {
                            popUpTo(Routes.Home.route) { inclusive = false }
                        }
                    },
                    onHome = {
                        pendingResult = null
                        navController.navigate(Routes.Home.route) {
                            popUpTo(Routes.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.History.route) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.Leaderboard.route) {
            LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}