package id.ac.pnm.quizbattleapp.data.repository

import id.ac.pnm.quizbattleapp.data.local.QuestionDao
import id.ac.pnm.quizbattleapp.data.model.Question
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val questionDao: QuestionDao
) {
    suspend fun getSoloQuestions(limit: Int = 10): List<Question> {
        // Cek jika kosong, isi dummy data
        if (questionDao.getQuestionCount() == 0) {
            populateDummyData()
        }
        return questionDao.getRandomQuestions(limit)
    }

    private suspend fun populateDummyData() {
        val dummyQuestions = listOf(
            Question(
                text = "Apa ibu kota dari negara Indonesia?",
                options = listOf("Jakarta", "Surabaya", "Bandung", "Medan"),
                correctAnswerIndex = 0
            ),
            Question(
                text = "Berapa hasil dari 5 + 7?",
                options = listOf("10", "11", "12", "13"),
                correctAnswerIndex = 2
            ),
            Question(
                text = "Hewan apa yang bernapas dengan insang?",
                options = listOf("Kucing", "Ikan", "Burung", "Ular"),
                correctAnswerIndex = 1
            ),
            Question(
                text = "Siapa penemu lampu pijar?",
                options = listOf("Nikola Tesla", "Albert Einstein", "Thomas Alva Edison", "Isaac Newton"),
                correctAnswerIndex = 2
            ),
            Question(
                text = "Warna bendera negara Indonesia adalah?",
                options = listOf("Merah Putih", "Putih Merah", "Merah Biru", "Biru Merah"),
                correctAnswerIndex = 0
            )
        )
        questionDao.insertAll(dummyQuestions)
    }
}
