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
    val selectedIndex: Int?         = null,   // jawaban yang dipilih user ronde ini
    val isFinished: Boolean         = false,
    val result: GameResult?         = null,
    val error: String?              = null
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val roomRepository: RoomRepository,
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val myUid: String  = auth.currentUser?.uid.orEmpty()
    private val myName: String = auth.currentUser?.displayName.orEmpty()

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    // Referensi Firebase untuk sesi game ini
    private val gameRef    = db.getReference("game_sessions").child(roomId)
    private val myRef      = gameRef.child("scores").child(myUid)
    private val timerJob   get() = _timerJob
    private var _timerJob: kotlinx.coroutines.Job? = null

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            _state.value = GameUiState(isLoading = true)

            // Ambil soal — gunakan roomId sebagai seed agar soal sama untuk kedua player
            // Workaround: simpan urutan soal di Firebase saat room dibuat, atau gunakan soal fixed
            val questions = quizRepository.getSoloQuestions(10)

            // Inisialisasi skor saya di Firebase
            myRef.setValue(0).await()

            // Observe skor lawan
            observeOpponentScore()

            // Observe status room (jika host disconnect dll)
            observeRoomStatus()

            _state.value = GameUiState(
                isLoading = false,
                questions = questions
            )

            startTimer()
        }
    }

    // ── Timer per soal ────────────────────────────────────────────────────
    private fun startTimer() {
        _timerJob?.cancel()
        _timerJob = viewModelScope.launch {
            for (t in 15 downTo 0) {
                _state.value = _state.value.copy(timeLeft = t)
                kotlinx.coroutines.delay(1000)
            }
            // Waktu habis → anggap salah, lanjut soal berikutnya
            moveToNext(answered = false)
        }
    }

    // ── Jawab soal ───────────────────────────────────────────────────────
    fun answerQuestion(selectedIndex: Int) {
        val s = _state.value
        if (s.selectedIndex != null || s.isFinished || s.isLoading) return

        _timerJob?.cancel()

        val isCorrect = selectedIndex == s.questions[s.currentIndex].correctAnswerIndex
        val newScore  = if (isCorrect) s.myScore + 20 else s.myScore

        _state.value = s.copy(selectedIndex = selectedIndex, myScore = newScore)

        // Push skor ke Firebase
        viewModelScope.launch { myRef.setValue(newScore).await() }

        // Jeda 1 detik agar user lihat hasil, lalu lanjut
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            moveToNext(answered = true)
        }
    }

    private fun moveToNext(answered: Boolean) {
        val s = _state.value
        val nextIndex = s.currentIndex + 1

        if (nextIndex >= s.questions.size) {
            finishGame()
            return
        }

        _state.value = s.copy(
            currentIndex  = nextIndex,
            selectedIndex = null,
            timeLeft      = 15
        )
        startTimer()
    }

    private fun finishGame() {
        _timerJob?.cancel()
        val s = _state.value

        val correctAnswers = s.questions.indices.count { i ->
            // Tidak bisa rekonstruksi jawaban per soal dari state ini
            // Estimasi dari skor: setiap benar +20
            false // placeholder — lihat catatan di bawah
        }

        val result = GameResult(
            score          = s.myScore,
            totalQuestions = s.questions.size,
            correctAnswers = s.myScore / 20,  // estimasi dari skor
            mode           = "online",
            opponentName   = s.opponentName,
            opponentScore  = s.opponentScore,
            isWinner       = s.myScore > s.opponentScore
        )

        _state.value = s.copy(isFinished = true, result = result)

        // Tandai room selesai
        viewModelScope.launch {
            roomRepository.updateStatus(roomId, RoomStatus.FINISHED)
        }
    }

    // ── Observe skor lawan dari Firebase ─────────────────────────────────
    private fun observeOpponentScore() {
        viewModelScope.launch {
            // Ambil data room untuk tahu uid lawan
            val roomSnapshot = db.getReference("rooms").child(roomId).get().await()
            val player1Uid   = roomSnapshot.child("player1").child("uid").getValue(String::class.java).orEmpty()
            val player2Uid   = roomSnapshot.child("player2").child("uid").getValue(String::class.java).orEmpty()
            val opponentUid  = if (myUid == player1Uid) player2Uid else player1Uid
            val opponentName = if (myUid == player1Uid)
                roomSnapshot.child("player2").child("displayName").getValue(String::class.java).orEmpty()
            else
                roomSnapshot.child("player1").child("displayName").getValue(String::class.java).orEmpty()

            _state.value = _state.value.copy(opponentName = opponentName)

            // Listen skor lawan real-time
            gameRef.child("scores").child(opponentUid)
                .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val opScore = snapshot.getValue(Int::class.java) ?: 0
                        _state.value = _state.value.copy(opponentScore = opScore)
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
                })
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