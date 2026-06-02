package id.ac.pnm.quizbattleapp.data.repository

import id.ac.pnm.quizbattleapp.data.local.GameHistoryDao
import id.ac.pnm.quizbattleapp.data.model.GameHistory
import id.ac.pnm.quizbattleapp.data.model.GameResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class HistoryRepository @Inject constructor(
    private val dao: GameHistoryDao
) {
    fun getHistory(): Flow<List<GameHistory>> = dao.getAll()
    suspend fun saveResult(result: GameResult) {
        dao.insert(
            GameHistory(
                mode = result.mode,
                myScore = result.score,
                opponentScore = result.opponentScore,
                opponentName = result.opponentName,
                correctAnswers = result.correctAnswers,
                totalQuestions = result.totalQuestions,
                isWinner = result.isWinner
            )
        )
    }
}