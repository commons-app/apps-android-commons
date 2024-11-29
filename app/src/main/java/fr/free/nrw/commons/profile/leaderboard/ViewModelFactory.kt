package fr.free.nrw.commons.profile.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import javax.inject.Inject


/**
 * This class extends the ViewModelProvider.Factory and creates a ViewModelFactory class
 * for leaderboardListViewModel
 */
class ViewModelFactory @Inject constructor(
    private val okHttpJsonApiClient: OkHttpJsonApiClient,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        if (modelClass.isAssignableFrom(LeaderboardListViewModel::class.java)) {
            LeaderboardListViewModel(okHttpJsonApiClient, sessionManager) as T
        } else {
            throw IllegalArgumentException("Unknown class name")
        }
}
