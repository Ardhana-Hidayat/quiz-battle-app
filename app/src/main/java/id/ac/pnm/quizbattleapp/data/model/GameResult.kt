package id.ac.pnm.quizbattleapp.data.model

data class GameResult(
    val score: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val mode: String,           // "solo" | "online"
    val opponentName: String = "",
    val opponentScore: Int  = 0,
    val isWinner: Boolean   = false
)