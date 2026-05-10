package id.ac.pnm.quizbattleapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import id.ac.pnm.quizbattleapp.data.datastore.UserSessionDataStore
import id.ac.pnm.quizbattleapp.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import id.ac.pnm.quizbattleapp.core.util.Result

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val sessionDataStore: UserSessionDataStore
) : AuthRepository {

    override val currentUser: User?
        get() = auth.currentUser?.toDomain()

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toDomain()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Result<User> = try {
        val user = auth.signInWithEmailAndPassword(email, password).await().user!!.toDomain()
        sessionDataStore.saveSession(user)
        Result.Success(user)
    } catch (e: Exception) {
        Result.Error(e.toReadableMessage())
    }

    override suspend fun register(
        email: String, password: String, name: String
    ): Result<User> = try {
        val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user!!
        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(name).build()
        ).await()
        val user = firebaseUser.toDomain().copy(displayName = name)
        sessionDataStore.saveSession(user)
        Result.Success(user)
    } catch (e: Exception) {
        Result.Error(e.toReadableMessage())
    }

    override suspend fun logout() {
        auth.signOut()
        sessionDataStore.clearSession()
    }

    private fun FirebaseUser.toDomain() = User(
        uid = uid, email = email.orEmpty(), displayName = displayName.orEmpty()
    )

    // Terjemahkan Firebase error code ke pesan yang ramah pengguna
    private fun Exception.toReadableMessage(): String = when {
        message?.contains("email address is already in use") == true ->
            "Email sudah terdaftar, silakan login"
        message?.contains("no user record") == true ->
            "Akun tidak ditemukan"
        message?.contains("password is invalid") == true ||
                message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
            "Email atau password salah"
        message?.contains("network") == true ->
            "Tidak ada koneksi internet"
        else -> message ?: "Terjadi kesalahan, coba lagi"
    }
}