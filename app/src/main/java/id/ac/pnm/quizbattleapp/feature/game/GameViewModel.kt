package id.ac.pnm.quizbattleapp.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.GameResult
import id.ac.pnm.quizbattleapp.data.model.Question
import id.ac.pnm.quizbattleapp.data.model.RoomStatus
import id.ac.pnm.quizbattleapp.data.repository.LeaderboardRepository
import id.ac.pnm.quizbattleapp.data.repository.QuizRepository
import id.ac.pnm.quizbattleapp.data.repository.RoomRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class GameUiState(
    val isLoading: Boolean          = true,
    val questions: List<Question>   = emptyList(),
    val currentIndex: Int           = 0,
    val myScore: Int                = 0,
    val opponentScore: Int          = 0,
    val opponentName: String        = "",
    val timeLeft: Int               = 15,
    val selectedIndex: Int?         = null,   
    val isFinished: Boolean         = false,
    val result: GameResult?         = null,
    val error: String?              = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val roomRepository: RoomRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val myUid: String  = auth.currentUser?.uid.orEmpty()
    private val myName: String = auth.currentUser?.displayName.orEmpty()

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val gameRef    = db.getReference("game_sessions").child(roomId)
    private val myRef      = gameRef.child("scores").child(myUid)
    private var _timerJob: kotlinx.coroutines.Job? = null

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            _state.value = GameUiState(isLoading = true)

            // mengambil data room
            val room = roomRepository.getRoom(roomId)
            
            if (room == null || room.questions.isEmpty()) {
                _state.value = GameUiState(isLoading = false, error = "Gagal memuat soal dari room.")
                return@launch
            }

            myRef.setValue(0).await()

            val opponentUid  = if (myUid == room.player1.uid) room.player2.uid else room.player1.uid
            val opponentName = if (myUid == room.player1.uid) room.player2.displayName else room.player1.displayName
            _state.value = _state.value.copy(opponentName = opponentName)

            // observasi skor dan status secara real-time
            observeOpponentScore(opponentUid)
            observeRoomStatus()

            _state.value = _state.value.copy(
                isLoading = false,
                questions = room.questions
            )

            startTimer()
        }
    }

    private fun startTimer() {
        _timerJob?.cancel()
        _timerJob = viewModelScope.launch {
            for (t in 15 downTo 0) {
                _state.value = _state.value.copy(timeLeft = t)
                kotlinx.coroutines.delay(1000)
            }
            moveToNext()
        }
    }

    fun answerQuestion(selectedIndex: Int) {
        val s = _state.value
        if (s.selectedIndex != null || s.isFinished || s.isLoading) return

        _timerJob?.cancel()

        val isCorrect = selectedIndex == s.questions[s.currentIndex].correctAnswerIndex
        val newScore  = if (isCorrect) s.myScore + 20 else s.myScore

        _state.value = s.copy(selectedIndex = selectedIndex, myScore = newScore)

        viewModelScope.launch { myRef.setValue(newScore).await() }

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            moveToNext()
        }
    }

    private fun moveToNext() {
        val s = _state.value
        val nextIndex = s.currentIndex + 1

        if (nextIndex >= s.questions.size) {
            finishGame()
            return
        }

        _state.value = s.copy(currentIndex = nextIndex, selectedIndex = null, timeLeft = 15)
        startTimer()
    }

    private fun finishGame() {
        _timerJob?.cancel()
        val s = _state.value

        val result = GameResult(
            score          = s.myScore,
            totalQuestions = s.questions.size,
            correctAnswers = s.myScore / 20,
            mode           = "online",
            opponentName   = s.opponentName,
            opponentScore  = s.opponentScore,
            isWinner       = s.myScore > s.opponentScore
        )

        _state.value = s.copy(isFinished = true, result = result)

        viewModelScope.launch {
            db.getReference("history").child(myUid).push().setValue(
                mapOf(
                    "mode"           to result.mode,
                    "myScore"        to result.score,
                    "opponentScore"  to result.opponentScore,
                    "opponentName"   to result.opponentName,
                    "correctAnswers" to result.correctAnswers,
                    "totalQuestions" to result.totalQuestions,
                    "isWinner"       to result.isWinner,
                    "playedAt"       to System.currentTimeMillis()
                )
            ).await()

            leaderboardRepository.updateLeaderboard(
                uid = myUid,
                name = myName,
                score = result.score,
                isWin = result.isWinner,
                playedAt = System.currentTimeMillis()
            )
            
            roomRepository.updateStatus(roomId, RoomStatus.FINISHED)
            gameRef.removeValue().await()
        }
    }

    private fun observeOpponentScore(opponentUid: String) {
        viewModelScope.launch {

            roomRepository.observeOpponentScore(roomId, opponentUid).collect { score ->
                _state.value = _state.value.copy(opponentScore = score)
            }
        }
    }

    private fun observeRoomStatus() {
        viewModelScope.launch {
            roomRepository.observeRoom(roomId).collect { room ->
                room ?: return@collect
                if (room.status == RoomStatus.FINISHED && !_state.value.isFinished) {
                    finishGame()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _timerJob?.cancel()
    }
}