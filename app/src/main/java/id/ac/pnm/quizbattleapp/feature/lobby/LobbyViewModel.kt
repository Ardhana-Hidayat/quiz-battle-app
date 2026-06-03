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

    private fun listenToRoom(roomId: String) {
        viewModelScope.launch {
            roomRepo.observeRoom(roomId).collect { room ->
                if (room == null) {
                    _state.value = LobbyUiState.Error("Room telah dibatalkan atau ditutup.")
                    return@collect
                }

                _state.value = when (room.status) {
                    RoomStatus.PLAYING  -> LobbyUiState.NavigateToGame(roomId)
                    RoomStatus.READY    -> LobbyUiState.Ready(room)    // ← andalkan status, bukan bothReady
                    else                -> LobbyUiState.Waiting(room)
                }
            }
        }
    }

    fun cancelRoom() {
        val currentRoom = when (val s = _state.value) {
            is LobbyUiState.Waiting -> s.room
            is LobbyUiState.Ready -> s.room
            else -> null
        }
        
        viewModelScope.launch {
            currentRoom?.let { room ->
                if (room.player1.uid == currentUid) {
                    // Jika kita pembuat room, hapus room dari Firebase
                    roomRepo.deleteRoom(room.roomId)
                }
            }
            _state.value = LobbyUiState.Idle
        }
    }

    fun backToList() {
        // Hanya reset state, TIDAK hapus room
        _state.value = LobbyUiState.Idle
    }

    fun cancelAndDeleteRoom() {
        // Hapus room DAN reset state
        val currentRoom = when (val s = _state.value) {
            is LobbyUiState.Waiting -> s.room
            is LobbyUiState.Ready   -> s.room
            else -> null
        }
        viewModelScope.launch {
            currentRoom?.let { room ->
                if (room.player1.uid == currentUid) {
                    roomRepo.deleteRoom(room.roomId)
                }
            }
            _state.value = LobbyUiState.Idle
        }
    }

    fun startGame(roomId: String) {
        viewModelScope.launch { roomRepo.updateStatus(roomId, RoomStatus.PLAYING) }
    }

    fun resetState() { _state.value = LobbyUiState.Idle }
}