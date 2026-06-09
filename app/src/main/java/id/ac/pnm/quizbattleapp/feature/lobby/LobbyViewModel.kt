package id.ac.pnm.quizbattleapp.feature.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.GameRoom
import id.ac.pnm.quizbattleapp.data.model.RoomStatus
import id.ac.pnm.quizbattleapp.data.repository.RoomRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LobbyUiState {
    object Idle                                   : LobbyUiState()
    object Loading                                : LobbyUiState()
    data class Waiting(val room: GameRoom)        : LobbyUiState()
    data class Ready(val room: GameRoom)          : LobbyUiState()
    data class Error(val message: String)         : LobbyUiState()
    data class NavigateToGame(val roomId: String) : LobbyUiState()
}

@HiltViewModel
class LobbyViewModel @Inject constructor(
    private val roomRepo: RoomRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow<LobbyUiState>(LobbyUiState.Idle)
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    val availableRooms: StateFlow<List<GameRoom>> = roomRepo
        .observeAvailableRooms()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUid: String get() = auth.currentUser?.uid.orEmpty()

    private var listenJob: Job? = null
    private var deleteJob: Job? = null

    // buat room/lobby
    fun createRoom() {
        viewModelScope.launch {
            _state.value = LobbyUiState.Loading
            roomRepo.createRoom().fold(
                onSuccess = { room ->
                    _state.value = LobbyUiState.Waiting(room)
                    listenToRoom(room.roomId)
                },
                onFailure = {
                    _state.value = LobbyUiState.Error(it.message ?: "Gagal membuat room")
                }
            )
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            _state.value = LobbyUiState.Loading
            roomRepo.joinRoom(roomId).fold(
                onSuccess = { room ->
                    _state.value = LobbyUiState.Ready(room)
                    listenToRoom(room.roomId)
                },
                onFailure = {
                    _state.value = LobbyUiState.Error(it.message ?: "Gagal bergabung")
                }
            )
        }
    }

    fun joinRoomByCode(code: String) {
        val formatted = code.trim().uppercase()
        if (formatted.length != 7) {  
            _state.value = LobbyUiState.Error("Kode room tidak valid.")
            return
        }
        joinRoom(formatted)
    }

    // pantau perubahan room
    private fun listenToRoom(roomId: String) {
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            roomRepo.observeRoom(roomId).collect { room ->
                if (room == null) {
                    _state.value = LobbyUiState.Error("Room telah dibatalkan atau ditutup.")
                    return@collect
                }
                _state.value = when (room.status) {
                    RoomStatus.PLAYING -> LobbyUiState.NavigateToGame(roomId)
                    RoomStatus.READY   -> LobbyUiState.Ready(room)
                    else               -> LobbyUiState.Waiting(room)
                }
            }
        }
    }

    fun backToList() {
        listenJob?.cancel()
        _state.value = LobbyUiState.Idle
    }

    // batalkan dan hapus room dari Firebase
    fun cancelAndDeleteRoom() {
        val currentRoom = when (val s = _state.value) {
            is LobbyUiState.Waiting -> s.room
            is LobbyUiState.Ready   -> s.room
            else                    -> null
        }
        
        listenJob?.cancel()

        deleteJob?.cancel() 
        
        _state.value = LobbyUiState.Idle
        
        currentRoom?.let { room ->
            if (room.player1.uid == currentUid) {

                deleteJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(60_000) 
                    
                    val latestRoom = roomRepo.getRoom(room.roomId)
                    if (latestRoom != null && !latestRoom.isFull && latestRoom.status == RoomStatus.WAITING) {
                        roomRepo.deleteRoom(room.roomId)
                    }
                }
            }
        }
    }

    // host mulai game
    fun startGame(roomId: String) {
        viewModelScope.launch { roomRepo.updateStatus(roomId, RoomStatus.PLAYING) }
    }
}