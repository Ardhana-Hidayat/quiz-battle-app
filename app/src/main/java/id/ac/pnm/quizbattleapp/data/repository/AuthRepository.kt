package id.ac.pnm.quizbattleapp.data.repository

import id.ac.pnm.quizbattleapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import id.ac.pnm.quizbattleapp.core.util.Result

interface AuthRepository {
    val currentUser: User?
    fun observeAuthState(): Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout()
}