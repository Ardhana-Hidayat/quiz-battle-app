package id.ac.pnm.quizbattleapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import id.ac.pnm.quizbattleapp.data.model.GameHistory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) {
    fun getHistory(): Flow<List<GameHistory>> = callbackFlow {
        val uid = auth.currentUser?.uid
            ?: run { trySend(emptyList()); close(); return@callbackFlow }

        val ref = db.getReference("history").child(uid).orderByChild("playedAt")

        val listener = ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    try {
                        GameHistory(
                            id            = child.key.orEmpty(),
                            mode          = child.child("mode").getValue(String::class.java) ?: "online",
                            myScore       = child.child("myScore").getValue(Int::class.java) ?: 0,
                            opponentScore = child.child("opponentScore").getValue(Int::class.java) ?: 0,
                            opponentName  = child.child("opponentName").getValue(String::class.java).orEmpty(),
                            correctAnswers = child.child("correctAnswers").getValue(Int::class.java) ?: 0,
                            totalQuestions = child.child("totalQuestions").getValue(Int::class.java) ?: 0,
                            isWinner      = child.child("isWinner").getValue(Boolean::class.java) ?: false,
                            playedAt      = child.child("playedAt").getValue(Long::class.java) ?: 0L
                        )
                    } catch (e: Exception) { null }
                }.sortedByDescending { it.playedAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
        })
        awaitClose { ref.removeEventListener(listener) }
    }
}