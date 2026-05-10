package id.ac.pnm.quizbattleapp.domain.usecase

import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import id.ac.pnm.quizbattleapp.domain.model.User
import id.ac.pnm.quizbattleapp.core.util.Result
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String, password: String, name: String
    ): Result<User> {
        if (name.isBlank())
            return Result.Error("Nama tidak boleh kosong")
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return Result.Error("Format email tidak valid")
        if (password.length < 6)
            return Result.Error("Password minimal 6 karakter")
        return authRepository.register(email.trim(), password, name.trim())
    }
}