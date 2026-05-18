package id.ac.pnm.quizbattleapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = repo.observeAuthState()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repo.currentUser != null)

    fun login(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repo.login(email, password).fold(
                onSuccess = { _state.value = AuthState.Success },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Login gagal") }
            )
        }
    }

    fun register(email: String, password: String, name: String) {
        if (name.isBlank()) { _state.value = AuthState.Error("Nama tidak boleh kosong"); return }
        if (!validate(email, password)) return
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repo.register(email, password, name).fold(
                onSuccess = { _state.value = AuthState.Success },
                onFailure = { _state.value = AuthState.Error(it.message ?: "Register gagal") }
            )
        }
    }

    fun logout() = repo.logout()

    fun resetState() { _state.value = AuthState.Idle }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank()    -> { _state.value = AuthState.Error("Email tidak boleh kosong"); false }
            password.length < 6 -> { _state.value = AuthState.Error("Password minimal 6 karakter"); false }
            else -> true
        }
    }
}