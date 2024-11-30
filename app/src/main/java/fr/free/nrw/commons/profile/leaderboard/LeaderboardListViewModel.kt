package fr.free.nrw.commons.profile.leaderboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LoadingStatus
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE

/**
 * Extends the ViewModel class and creates the LeaderboardList View Model
 */
class LeaderboardListViewModel(
    okHttpJsonApiClient: OkHttpJsonApiClient,
    sessionManager: SessionManager
) : ViewModel() {
    private val dataSourceFactory = DataSourceFactory(okHttpJsonApiClient, sessionManager)

    val listLiveData: LiveData<PagedList<LeaderboardList>> = LivePagedListBuilder(
        dataSourceFactory,
        PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(PAGE_SIZE)
            .setPageSize(PAGE_SIZE).build()
    ).build()

    val progressLoadStatus: LiveData<LoadingStatus> =
        dataSourceFactory.mutableLiveData.switchMap { it.progressLiveStatus }

    /**
     * Refreshes the paged list with the new params and starts the loading of new data
     */
    fun refresh(duration: String?, category: String?, limit: Int, offset: Int) {
        dataSourceFactory.duration = duration
        dataSourceFactory.category = category
        dataSourceFactory.limit = limit
        dataSourceFactory.offset = offset
        dataSourceFactory.mutableLiveData.value!!.invalidate()
    }

    /**
     * Sets the new params for the paged list API calls
     */
    fun setParams(duration: String?, category: String?, limit: Int, offset: Int) {
        dataSourceFactory.duration = duration
        dataSourceFactory.category = category
        dataSourceFactory.limit = limit
        dataSourceFactory.offset = offset
    }
}
