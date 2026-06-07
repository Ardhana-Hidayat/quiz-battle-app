package id.ac.pnm.quizbattleapp.feature.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.ac.pnm.quizbattleapp.data.model.LeaderboardEntry
import id.ac.pnm.quizbattleapp.data.repository.LeaderboardRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repo: LeaderboardRepository
) : ViewModel() {

    val entries: StateFlow<List<LeaderboardEntry>> = repo.getTopLeaderboard()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isLoading: StateFlow<Boolean> = entries
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}