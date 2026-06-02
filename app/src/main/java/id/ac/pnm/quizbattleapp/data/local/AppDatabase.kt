package id.ac.pnm.quizbattleapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.ac.pnm.quizbattleapp.data.model.GameHistory
import id.ac.pnm.quizbattleapp.data.model.Question

@Database(
    entities = [Question::class, GameHistory::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun gameHistoryDao(): GameHistoryDao
}