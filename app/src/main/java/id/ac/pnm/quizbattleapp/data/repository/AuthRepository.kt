package id.ac.pnm.quizbattleapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import id.ac.pnm.quizbattleapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    // Sesi aktif dari Firebase (persisten, tidak perlu DataStore)
    val currentUser: User? get() = auth.currentUser?.let {
        User(uid = it.uid, email = it.email.orEmpty(), displayName = it.displayName.orEmpty())
    }

    fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser?.let { u ->
                User(uid = u.uid, email = u.email.orEmpty(), displayName = u.displayName.orEmpty())
            })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        val u = auth.signInWithEmailAndPassword(email, password).await().user!!
        User(uid = u.uid, email = u.email.orEmpty(), displayName = u.displayName.orEmpty())
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(Exception(it.toMessage())) }
    )

    suspend fun register(email: String, password: String, name: String): Result<User> = runCatching {
        val u = auth.createUserWithEmailAndPassword(email, password).await().user!!
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        User(uid = u.uid, email = u.email.orEmpty(), displayName = name)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(Exception(it.toMessage())) }
    )

    fun logout() = auth.signOut()

    private fun Throwable.toMessage() = when {
        message?.contains("email address is already in use") == true -> "Email sudah terdaftar"
        message?.contains("INVALID_LOGIN_CREDENTIALS")       == true -> "Email atau password salah"
        message?.contains("network")                         == true -> "Tidak ada koneksi internet"
        else -> "Terjadi kesalahan, coba lagi"
    }
}