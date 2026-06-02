package id.ac.pnm.quizbattleapp.feature.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import id.ac.pnm.quizbattleapp.data.model.GameRoom
import id.ac.pnm.quizbattleapp.data.model.RoomStatus
import id.ac.pnm.quizbattleapp.data.repository.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LobbyUiState {
    object Idle                                      : LobbyUiState()
    object Loading                                   : LobbyUiState()
    data class Waiting(val room: GameRoom)           : LobbyUiState()
    data class Ready(val room: GameRoom)             : LobbyUiState()
    data class Error(val message: String)            : LobbyUiState()
    data class NavigateToGame(val roomId: String)    : LobbyUiState()
}

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val roomRepo: RoomRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<LobbyUiState>(LobbyUiState.Idle)
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    // List room yang tersedia — observe real-time
    val availableRooms: StateFlow<List<GameRoom>> = roomRepo
        .observeAvailableRooms()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUid: String get() = auth.currentUser?.uid.orEmpty()

    // ── Buat room baru (via FAB) ───────────────────────────────────────────
    fun createRoom() {
        viewModelScope.launch {
            _state.value = LobbyUiState.Loading
            roomRepo.createRoom().fold(
                onSuccess = { room ->
                    _state.value = LobbyUiState.Waiting(room)
                    listenToRoom(room.roomId)
                },
                onFailure = { _state.value = LobbyUiState.Error(it.message ?: "Gagal membuat room") }
            )
        }
    }

    // ── Tap room dari list → join ──────────────────────────────────────────
    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            _state.value = LobbyUiState.Loading
            roomRepo.joinRoom(roomId).fold(
                onSuccess = { room ->
                    _state.value = LobbyUiState.Ready(room)
                    listenToRoom(room.roomId)
                },
                onFailure = { _state.value = LobbyUiState.Error(it.message ?: "Gagal bergabung") }
            )
        }
    }

    // ── Observe room real-time setelah create / join ───────────────────────
    private fun listenToRoom(roomId: String) {
        viewModelScope.launch {
            roomRepo.observeRoom(roomId).collect { room ->
                room ?: return@collect
                _state.value = when {
                    room.status == RoomStatus.PLAYING -> LobbyUiState.NavigateToGame(roomId)
                    room.bothReady                    -> LobbyUiState.Ready(room)
                    else                              -> LobbyUiState.Waiting(room)
                }
            }
        }
    }

    fun startGame(roomId: String) {
        viewModelScope.launch { roomRepo.updateStatus(roomId, RoomStatus.PLAYING) }
    }

    fun resetState() { _state.value = LobbyUiState.Idle }
}