package id.ac.pnm.quizbattleapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.repository.AuthRepository
import id.ac.pnm.quizbattleapp.data.model.GameMode
import id.ac.pnm.quizbattleapp.data.model.User
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
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = authRepository.observeAuthState()
        .map { HomeUiState(user = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun logout() = authRepository.logout()
}