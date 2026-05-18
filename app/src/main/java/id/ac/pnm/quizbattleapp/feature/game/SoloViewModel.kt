package id.ac.pnm.quizbattleapp.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.Question
import id.ac.pnm.quizbattleapp.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoloUiState(
    val questions: List<Question> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true
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
            val questions = quizRepository.getSoloQuestions(5)
            _uiState.value = SoloUiState(
                questions = questions,
                isLoading = false
            )
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val currentState = _uiState.value
        if (currentState.isFinished || currentState.isLoading) return

        val currentQuestion = currentState.questions[currentState.currentQuestionIndex]
        val isCorrect = selectedIndex == currentQuestion.correctAnswerIndex
        val newScore = if (isCorrect) currentState.score + 20 else currentState.score

        if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
            // Lanjut ke soal berikutnya
            _uiState.value = currentState.copy(
                currentQuestionIndex = currentState.currentQuestionIndex + 1,
                score = newScore
            )
        } else {
            // Selesai
            _uiState.value = currentState.copy(
                score = newScore,
                isFinished = true
            )
        }
    }

    fun restartGame() {
        startGame()
    }
}
