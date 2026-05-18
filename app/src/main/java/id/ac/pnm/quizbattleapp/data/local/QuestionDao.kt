package id.ac.pnm.quizbattleapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.ac.pnm.quizbattleapp.data.model.Question

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<Question>)
    
    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int
}
