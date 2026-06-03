package id.ac.pnm.quizbattleapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import id.ac.pnm.quizbattleapp.data.model.GameRoom
import id.ac.pnm.quizbattleapp.data.model.RoomPlayer
import id.ac.pnm.quizbattleapp.data.model.RoomStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    private val rooms = db.getReference("rooms")

    private suspend fun generateUniqueRoomId(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    repeat(10) { // maksimal 10 kali coba
        val candidate = "QB-" + (1..4).map { chars.random() }.joinToString("")
        val snapshot = rooms.child(candidate).get().await()
        if (!snapshot.exists()) return candidate
    }
        error("Gagal membuat room ID unik, coba lagi")
    }

    suspend fun getRoom(roomId: String): GameRoom? {
        val snapshot = rooms.child(roomId).get().await()
        return snapshot.getValue(GameRoom::class.java)
    }

    suspend fun createRoom(): Result<GameRoom> = runCatching {
        val user    = auth.currentUser ?: error("User belum login")
        val roomId  = generateUniqueRoomId()

        // Ambil ID soal dari Firebase, acak, ambil 10
        val snapshot    = db.getReference("questions").get().await()
        val selectedIds = snapshot.children
            .mapNotNull { it.child("id").getValue(Int::class.java) }
            .shuffled()
            .take(10)

        val player1 = RoomPlayer(uid = user.uid, displayName = user.displayName.orEmpty(), isReady = true)
        val room    = GameRoom(
            roomId      = roomId,
            status      = RoomStatus.WAITING,
            createdAt   = System.currentTimeMillis(),
            player1     = player1,
            questionIds = selectedIds   // ← urutan soal ditentukan host
        )
        rooms.child(roomId).setValue(room).await()
        room
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(Exception(it.message ?: "Gagal membuat room")) }
    )

    suspend fun joinRoom(roomId: String): Result<GameRoom> = runCatching {
        val user     = auth.currentUser ?: error("User belum login")
        val snapshot = rooms.child(roomId).get().await()
        val room     = snapshot.getValue(GameRoom::class.java) ?: error("Room tidak ditemukan")

        // Jika yang klik adalah Host sendiri, biarkan dia masuk kembali
        if (room.player1.uid == user.uid) {
            return@runCatching room
        }

        when {
            room.isFull                       -> error("Room sudah penuh")
            room.status != RoomStatus.WAITING -> error("Room tidak tersedia")
        }

        val player2 = RoomPlayer(uid = user.uid, displayName = user.displayName.orEmpty(), isReady = true)
        rooms.child(roomId).child("player2").setValue(player2).await()

        rooms.child(roomId).child("status").setValue(RoomStatus.READY).await()

        room.copy(player2 = player2, status = RoomStatus.READY)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(Exception(it.message ?: "Gagal bergabung")) }
    )

    fun observeRoom(roomId: String): Flow<GameRoom?> = callbackFlow {
        val ref      = rooms.child(roomId)
        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(GameRoom::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAvailableRooms(): Flow<List<GameRoom>> = callbackFlow {
        val query = rooms
            .orderByChild("status")
            .equalTo(RoomStatus.WAITING)

        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { it.getValue(GameRoom::class.java) }
                    .filter { room ->
                        !room.isFull // Hanya filter room penuh. Filter room sendiri dihapus!
                    }
                    .sortedByDescending { it.createdAt }  // terbaru di atas
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun deleteRoom(roomId: String) {
        rooms.child(roomId).removeValue().await()
    }

    suspend fun updateStatus(roomId: String, status: String) {
        rooms.child(roomId).child("status").setValue(status).await()
    }
}