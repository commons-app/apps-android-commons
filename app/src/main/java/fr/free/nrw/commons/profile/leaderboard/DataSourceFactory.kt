package fr.free.nrw.commons.profile.leaderboard

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient

/**
 * This class will create a new instance of the data source class on pagination
 */
class DataSourceFactory(
    private val okHttpJsonApiClient: OkHttpJsonApiClient,
    private val sessionManager: SessionManager
) : DataSource.Factory<Int, LeaderboardList>() {
    val mutableLiveData: MutableLiveData<DataSourceClass> = MutableLiveData()
    var duration: String? = null
    var category: String? = null
    var limit: Int = 0
    var offset: Int = 0

    /**
     * Creates the new instance of data source class
     */
    override fun create(): DataSource<Int, LeaderboardList> = DataSourceClass(
        okHttpJsonApiClient, sessionManager, duration, category, limit, offset
    ).also { mutableLiveData.postValue(it) }
}
