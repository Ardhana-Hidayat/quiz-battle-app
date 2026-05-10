package id.ac.pnm.quizbattleapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.ac.pnm.quizbattleapp.feature.auth.AuthScreen
import id.ac.pnm.quizbattleapp.feature.auth.AuthViewModel
import id.ac.pnm.quizbattleapp.feature.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {

    // Tentukan start destination berdasarkan status login
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val startDestination = if (isLoggedIn) Routes.Home.route else Routes.Auth.route

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Home.route) {
            HomeScreen(
                onOnlineBattle = { navController.navigate(Routes.Lobby.route) },
                onSoloTraining = { navController.navigate(Routes.Game.route) },
                onLogout = {
                    navController.navigate(Routes.Auth.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}