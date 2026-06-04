package id.ac.pnm.quizbattleapp.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.GameResult
import id.ac.pnm.quizbattleapp.data.model.Question
import id.ac.pnm.quizbattleapp.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoloUiState(
    val questions: List<Question>  = emptyList(),
    val currentQuestionIndex: Int  = 0,
    val score: Int                 = 0,
    val correctAnswers: Int        = 0,   // ← tambah ini
    val isFinished: Boolean        = false,
    val isLoading: Boolean         = true,
    val result: GameResult?        = null  // ← tambah ini
)

@HiltViewModel
class SoloViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SoloUiState())
    val uiState: StateFlow<SoloUiState> = _uiState.asStateFlow()

    init {
        startGame()
    }

    private fun startGame() {
        viewModelScope.launch {
            _uiState.value = SoloUiState(isLoading = true)
            // Ambil 5 pertanyaan untuk sesi ini
            val questions = quizRepository.getSoloQuestions(10)
            _uiState.value = SoloUiState(
                questions = questions,
                isLoading = false
            )
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val s = _uiState.value
        if (s.isFinished || s.isLoading) return

        val currentQuestion = s.questions[s.currentQuestionIndex]
        val isCorrect       = selectedIndex == currentQuestion.correctAnswerIndex
        val newScore        = if (isCorrect) s.score + 20 else s.score
        val newCorrect      = if (isCorrect) s.correctAnswers + 1 else s.correctAnswers

        if (s.currentQuestionIndex < s.questions.size - 1) {
            _uiState.value = s.copy(
                currentQuestionIndex = s.currentQuestionIndex + 1,
                score          = newScore,
                correctAnswers = newCorrect
            )
        } else {
            val result = GameResult(
                score          = newScore,
                totalQuestions = s.questions.size,
                correctAnswers = newCorrect,
                mode           = "solo"
            )
            _uiState.value = s.copy(
                score          = newScore,
                correctAnswers = newCorrect,
                isFinished     = true,
                result         = result
            )
        }
    }

    fun restartGame() {
        startGame()
    }
}
