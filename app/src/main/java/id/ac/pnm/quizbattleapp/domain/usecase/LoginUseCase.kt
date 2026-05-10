package id.ac.pnm.quizbattleapp.domain.usecase

import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import id.ac.pnm.quizbattleapp.domain.model.User
import id.ac.pnm.quizbattleapp.core.util.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank() || password.isBlank())
            return Result.Error("Email dan password tidak boleh kosong")
        if (password.length < 6)
            return Result.Error("Password minimal 6 karakter")
        return authRepository.login(email.trim(), password)
    }
}