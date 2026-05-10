package id.ac.pnm.quizbattleapp.domain.usecase

import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import id.ac.pnm.quizbattleapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.observeAuthState()
}