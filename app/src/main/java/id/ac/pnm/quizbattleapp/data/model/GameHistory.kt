package id.ac.pnm.quizbattleapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "game_history")
data class GameHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mode: String, // "solo" | "online"
    val myScore: Int,
    val opponentScore: Int = 0,
    val opponentName: String = "",
    val correctAnswers: Int,
    val totalQuestions: Int,
    val isWinner: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
)
