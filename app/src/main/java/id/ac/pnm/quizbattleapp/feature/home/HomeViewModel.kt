package id.ac.pnm.quizbattleapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import id.ac.pnm.quizbattleapp.domain.model.GameMode
import id.ac.pnm.quizbattleapp.domain.model.User
import id.ac.pnm.quizbattleapp.domain.usecase.GetCurrentUserUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: User?        = null,
    val gameModes: List<GameMode> = GameMode.values().toList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}