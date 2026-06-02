package id.ac.pnm.quizbattleapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.GameHistory
import id.ac.pnm.quizbattleapp.data.repository.HistoryRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
data class HistoryUiState(
    val items: List<GameHistory> = emptyList(),
    val isLoading: Boolean = true
)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = repo.getHistory()
        .map { HistoryUiState(items = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HistoryUiState(isLoading = true)
        )
}
