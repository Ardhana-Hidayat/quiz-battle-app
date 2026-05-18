package id.ac.pnm.quizbattleapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val options: List<String>, // Akan diconvert dengan TypeConverter
    val correctAnswerIndex: Int,
    val difficulty: String = "Easy"
)
