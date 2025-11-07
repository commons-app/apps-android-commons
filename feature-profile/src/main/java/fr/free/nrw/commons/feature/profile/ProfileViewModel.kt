package fr.free.nrw.commons.feature.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState = _uiState.asStateFlow()

    private fun createInitialState(): ProfileState {
        return ProfileState(
            userName = "username",
            userAvatarUrl = "", // Will be replaced with a real avatar
            userRank = 1736,
            userContributions = 0,
            leaderboard = listOf(
                LeaderboardUser(1, "", "GeographBot", 29442),
                LeaderboardUser(2, "", "PantheraLeo1359531", 12214),
                LeaderboardUser(3, "", "Fabe56", 8074),
                LeaderboardUser(4, "", "Mr.Nostalgic", 3204),
                LeaderboardUser(5, "", "Ser Amantio di Nicolao", 1535),
                LeaderboardUser(6, "", "Tm", 1430)
            )
        )
    }
}