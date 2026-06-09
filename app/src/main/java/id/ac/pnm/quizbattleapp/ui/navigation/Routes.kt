package id.ac.pnm.quizbattleapp.ui.navigation

sealed class Routes(val route: String) {
    object Auth        : Routes("auth")
    object Home        : Routes("home")
    object Lobby       : Routes("lobby")
    object Game        : Routes("game")
    object Result      : Routes("result")
    object Solo        : Routes("solo")
    object History     : Routes("history")
    object Leaderboard : Routes("leaderboard")
}