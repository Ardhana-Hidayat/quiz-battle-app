package id.ac.pnm.quizbattleapp.data.model

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val bestScore: Int = 0,
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val lastPlayedAt: Long = 0
)