package id.ac.pnm.quizbattleapp.data.model

data class GameHistory(
    val id: String = "",
    val mode: String = "solo", 
    val myScore: Int = 0,
    val opponentScore: Int = 0,
    val opponentName: String = "",
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val isWinner: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
)