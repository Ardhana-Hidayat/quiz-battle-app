package id.ac.pnm.quizbattleapp.feature.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    val selectedIndex: Int?         = null,   
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

            // Ambil questionIds dari room
            val roomSnapshot = db.getReference("rooms").child(roomId).get().await()
            val questionIds  = roomSnapshot.child("questionIds")
                .children
                .mapNotNull { it.getValue(Int::class.java) }

            if (questionIds.isEmpty()) {
                _state.value = GameUiState(isLoading = false, error = "Soal tidak ditemukan.")
                return@launch
            }

            // Ambil semua soal dari Firebase dengan mapping manual
            val allSnapshot  = db.getReference("questions").get().await()
            val allQuestions = allSnapshot.children.mapNotNull { child ->
                try {
                    val id                 = child.child("id").getValue(Int::class.java) ?: return@mapNotNull null
                    val text               = child.child("text").getValue(String::class.java) ?: return@mapNotNull null
                    val correctAnswerIndex = child.child("correctAnswerIndex").getValue(Int::class.java) ?: return@mapNotNull null
                    val difficulty         = child.child("difficulty").getValue(String::class.java) ?: "Easy"
                    val options            = child.child("options").children
                        .mapNotNull { it.getValue(String::class.java) }

                    Question(
                        id                 = id,
                        text               = text,
                        options            = options,
                        correctAnswerIndex = correctAnswerIndex,
                        difficulty         = difficulty
                    )
                } catch (e: Exception) { null }
            }.associateBy { it.id }

            // Susun soal sesuai urutan questionIds — SAMA untuk kedua player
            val questions = questionIds.mapNotNull { allQuestions[it] }

            if (questions.isEmpty()) {
                _state.value = GameUiState(isLoading = false, error = "Gagal memuat soal.")
                return@launch
            }

            myRef.setValue(0).await()
            observeOpponentScore()
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

            moveToNext()
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
            // Simpan ke history Firebase
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
            roomRepository.updateStatus(roomId, RoomStatus.FINISHED)
            gameRef.removeValue().await()
        }
    }

    // Tambah dua property di atas init {}
    private var opponentScoreListener: ValueEventListener? = null
    private var opponentScoreRef: DatabaseReference?       = null

    // Ganti fungsi observeOpponentScore():
    private fun observeOpponentScore() {
        viewModelScope.launch {
            val roomSnapshot = db.getReference("rooms").child(roomId).get().await()
            val player1Uid   = roomSnapshot.child("player1").child("uid").getValue(String::class.java).orEmpty()
            val player2Uid   = roomSnapshot.child("player2").child("uid").getValue(String::class.java).orEmpty()
            val opponentUid  = if (myUid == player1Uid) player2Uid else player1Uid
            val opponentName = if (myUid == player1Uid)
                roomSnapshot.child("player2").child("displayName").getValue(String::class.java).orEmpty()
            else
                roomSnapshot.child("player1").child("displayName").getValue(String::class.java).orEmpty()

            _state.value = _state.value.copy(opponentName = opponentName)

            opponentScoreListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val opScore = snapshot.getValue(Int::class.java) ?: 0
                    _state.value = _state.value.copy(opponentScore = opScore)
                }
                override fun onCancelled(error: DatabaseError) {}
            }

            opponentScoreRef = gameRef.child("scores").child(opponentUid)
            opponentScoreRef!!.addValueEventListener(opponentScoreListener!!)
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
        opponentScoreListener?.let { opponentScoreRef?.removeEventListener(it) }
    }
}