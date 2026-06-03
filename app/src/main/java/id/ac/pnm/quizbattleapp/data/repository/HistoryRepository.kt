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
                val list = snapshot.children
                    .mapNotNull { child ->
                        child.getValue(GameHistory::class.java)?.copy(id = child.key.orEmpty())
                    }
                    .sortedByDescending { it.playedAt }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { close(Exception(error.message)) }
        })
        awaitClose { ref.removeEventListener(listener) }
    }
}