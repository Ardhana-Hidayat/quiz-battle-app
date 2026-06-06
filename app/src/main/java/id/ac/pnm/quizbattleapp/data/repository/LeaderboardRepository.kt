package id.ac.pnm.quizbattleapp.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import id.ac.pnm.quizbattleapp.data.model.LeaderboardEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val db: FirebaseDatabase
) {

    fun getTopLeaderboard(limit: Int = 10): Flow<List<LeaderboardEntry>> = callbackFlow {
        val ref = db.getReference("leaderboard").orderByChild("bestScore")
        val listener = ref.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    try {
                        LeaderboardEntry(
                            uid         = child.key.orEmpty(),
                            name        = child.child("name").getValue(String::class.java).orEmpty(),
                            bestScore   = child.child("bestScore").getValue(Int::class.java) ?: 0,
                            totalScore  = child.child("totalScore").getValue(Int::class.java) ?: 0,
                            gamesPlayed = child.child("gamesPlayed").getValue(Int::class.java) ?: 0,
                            wins        = child.child("wins").getValue(Int::class.java) ?: 0,
                            lastPlayedAt = child.child("lastPlayedAt").getValue(Long::class.java) ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                .sortedByDescending { it.bestScore }
                .take(limit)
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        })

        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updateLeaderboard(
            uid: String,
            name: String,
            score: Int,
            isWin: Boolean,
            playedAt: Long
        ) {
            val ref      = db.getReference("leaderboard").child(uid)
            val snapshot = ref.get().await()
            val existing = if (snapshot.exists()) {
                try {
                    LeaderboardEntry(
                        uid         = uid,
                        name        = snapshot.child("name").getValue(String::class.java).orEmpty(),
                        bestScore   = snapshot.child("bestScore").getValue(Int::class.java) ?: 0,
                        totalScore  = snapshot.child("totalScore").getValue(Int::class.java) ?: 0,
                        gamesPlayed = snapshot.child("gamesPlayed").getValue(Int::class.java) ?: 0,
                        wins        = snapshot.child("wins").getValue(Int::class.java) ?: 0,
                        lastPlayedAt = snapshot.child("lastPlayedAt").getValue(Long::class.java) ?: 0L
                    )
                } catch (e: Exception) { null }
            } else null

            val updated = if (existing != null) {
                existing.copy(
                    name        = if (name.isBlank()) existing.name else name,
                    bestScore   = maxOf(existing.bestScore, score),
                    totalScore  = existing.totalScore + score,
                    gamesPlayed = existing.gamesPlayed + 1,
                    wins        = existing.wins + if (isWin) 1 else 0,
                    lastPlayedAt = playedAt
                )
            } else {
                LeaderboardEntry(
                    uid         = uid,
                    name        = name,
                    bestScore   = score,
                    totalScore  = score,
                    gamesPlayed = 1,
                    wins        = if (isWin) 1 else 0,
                    lastPlayedAt = playedAt
                )
            }

            ref.setValue(updated).await()
        }
}