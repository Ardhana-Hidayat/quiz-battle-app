package id.ac.pnm.quizbattleapp.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentFirebaseUser: FirebaseUser? get() = auth.currentUser

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun loginWithEmail(email: String, password: String): FirebaseUser {
        return auth.signInWithEmailAndPassword(email, password)
            .await()
            .user ?: throw Exception("Login gagal: user null")
    }

    suspend fun registerWithEmail(
        email: String, password: String, name: String
    ): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user   = result.user ?: throw Exception("Register gagal")

        // update display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        user.updateProfile(profileUpdates).await()
        return user
    }

    suspend fun loginWithGoogle(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return auth.signInWithCredential(credential)
            .await()
            .user ?: throw Exception("Google Sign-In gagal")
    }

    fun logout() = auth.signOut()
}