package id.ac.pnm.quizbattleapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.domain.model.User
import id.ac.pnm.quizbattleapp.domain.usecase.LoginUseCase
import id.ac.pnm.quizbattleapp.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import id.ac.pnm.quizbattleapp.core.util.Result
import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User)    : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository
        .observeAuthState()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = loginUseCase(email, password)) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(result.message)
                else              -> AuthUiState.Idle
            }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val result = registerUseCase(email, password, name)) {
                is Result.Success -> AuthUiState.Success(result.data)
                is Result.Error   -> AuthUiState.Error(result.message)
                else              -> AuthUiState.Idle
            }
        }
    }

    fun resetState() { _uiState.value = AuthUiState.Idle }
}