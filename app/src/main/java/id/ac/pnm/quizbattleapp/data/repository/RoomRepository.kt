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

    private fun parseGameRoom(snapshot: DataSnapshot): GameRoom? {
        return try {
            val roomId    = snapshot.child("roomId").getValue(String::class.java) ?: return null
            val status    = snapshot.child("status").getValue(String::class.java) ?: RoomStatus.WAITING
            val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L
            val player1 = RoomPlayer(
                uid         = snapshot.child("player1").child("uid").getValue(String::class.java).orEmpty(),
                displayName = snapshot.child("player1").child("displayName").getValue(String::class.java).orEmpty(),
                isReady     = snapshot.child("player1").child("isReady").getValue(Boolean::class.java) ?: false
            )
            val player2 = RoomPlayer(
                uid         = snapshot.child("player2").child("uid").getValue(String::class.java).orEmpty(),
                displayName = snapshot.child("player2").child("displayName").getValue(String::class.java).orEmpty(),
                isReady     = snapshot.child("player2").child("isReady").getValue(Boolean::class.java) ?: false
            )
            
            // MENGAMBIL LANGSUNG LIST OBJEK SOAL DARI ROOM
            val questions = snapshot.child("questions").children
                .mapNotNull { it.getValue(id.ac.pnm.quizbattleapp.data.model.Question::class.java) }
            GameRoom(
                roomId      = roomId,
                status      = status,
                createdAt   = createdAt,
                player1     = player1,
                player2     = player2,
                questions   = questions // Gunakan questions di sini
            )
        } catch (e: Exception) { null }
    }

    // ── Generate unique room ID ───────────────────────────────────────
    private suspend fun generateUniqueRoomId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        repeat(10) {
            val candidate = "QB-" + (1..4).map { chars.random() }.joinToString("")
            val snapshot  = rooms.child(candidate).get().await()
            if (!snapshot.exists()) return candidate
        }
        error("Gagal membuat room ID unik, coba lagi")
    }

    // ── CRUD ──────────────────────────────────────────────────────────
    suspend fun getRoom(roomId: String): GameRoom? {
        val snapshot = rooms.child(roomId).get().await()
        return parseGameRoom(snapshot)
    }

    suspend fun createRoom(): Result<GameRoom> = runCatching {
        val user       = auth.currentUser ?: error("User belum login")
        val roomId     = generateUniqueRoomId()
        val snapshot   = db.getReference("questions").get().await()
        
        // Ambil objek Question secara utuh
        val selectedQuestions = snapshot.children
            .mapNotNull { it.getValue(id.ac.pnm.quizbattleapp.data.model.Question::class.java) }
            .shuffled()
            .take(10)
        val player1 = RoomPlayer(uid = user.uid, displayName = user.displayName.orEmpty(), isReady = true)
        val room    = GameRoom(
            roomId      = roomId,
            status      = RoomStatus.WAITING,
            createdAt   = System.currentTimeMillis(),
            player1     = player1,
            questions   = selectedQuestions // Simpan soal utuh ke Firebase!
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
        val room     = parseGameRoom(snapshot) ?: error("Room tidak ditemukan")

        if (room.player1.uid == user.uid) return@runCatching room

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
                trySend(parseGameRoom(snapshot))
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeAvailableRooms(): Flow<List<GameRoom>> = callbackFlow {
        val query    = rooms.orderByChild("status").equalTo(RoomStatus.WAITING)
        val listener = query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children
                    .mapNotNull { parseGameRoom(it) }
                    .filter { !it.isFull }
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })
        awaitClose { query.removeEventListener(listener) }
    }

    fun observeOpponentScore(roomId: String, opponentUid: String): Flow<Int> = callbackFlow {
        val ref = db.getReference("game_sessions").child(roomId).child("scores").child(opponentUid)
        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue(Int::class.java) ?: 0
                trySend(score)
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun deleteRoom(roomId: String) {
        rooms.child(roomId).removeValue().await()
    }

    suspend fun updateStatus(roomId: String, status: String) {
        rooms.child(roomId).child("status").setValue(status).await()
    }
}