package id.ac.pnm.quizbattleapp.data.local

import androidx.room.*
import id.ac.pnm.quizbattleapp.data.model.GameHistory
import kotlinx.coroutines.flow.Flow
@Dao
interface GameHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: GameHistory)
    @Query("SELECT * FROM game_history ORDER BY playedAt DESC")
    fun getAll(): Flow<List<GameHistory>>
    @Query("DELETE FROM game_history")
    suspend fun deleteAll()
}
