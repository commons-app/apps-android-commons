package fr.free.nrw.commons.profile.leaderboard

import android.accounts.Account
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LoadingStatus.LOADING
import fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LoadingStatus.LOADED
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber
import java.util.Objects

/**
 * This class will call the leaderboard API to get new list when the pagination is performed
 */
class DataSourceClass(
    private val okHttpJsonApiClient: OkHttpJsonApiClient,
    private val sessionManager: SessionManager,
    private val duration: String?,
    private val category: String?,
    private val limit: Int,
    private val offset: Int
) : PageKeyedDataSource<Int, LeaderboardList>() {
    val progressLiveStatus: MutableLiveData<LeaderboardConstants.LoadingStatus> = MutableLiveData()
    private val compositeDisposable = CompositeDisposable()


    override fun loadInitial(
        params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, LeaderboardList?>
    ) {
        compositeDisposable.add(okHttpJsonApiClient.getLeaderboard(
            sessionManager.currentAccount?.name,
            duration,
            category,
            limit.toString(),
            offset.toString()
        ).doOnSubscribe { disposable: Disposable? ->
            compositeDisposable.add(disposable!!)
            progressLiveStatus.postValue(LOADING)
        }.subscribe({ response: LeaderboardResponse? ->
            if (response != null && response.status == 200) {
                progressLiveStatus.postValue(LOADED)
                callback.onResult(response.leaderboardList!!, null, response.limit)
            }
        }, { t: Throwable? ->
            Timber.e(t, "Fetching leaderboard statistics failed")
            progressLiveStatus.postValue(LOADING)
        }))
    }

    override fun loadBefore(
        params: LoadParams<Int>, callback: LoadCallback<Int, LeaderboardList?>
    ) = Unit

    override fun loadAfter(
        params: LoadParams<Int>, callback: LoadCallback<Int, LeaderboardList?>
    ) {
        compositeDisposable.add(okHttpJsonApiClient.getLeaderboard(
            Objects.requireNonNull<Account?>(sessionManager.currentAccount).name,
            duration,
            category,
            limit.toString(),
            params.key.toString()
        ).doOnSubscribe { disposable: Disposable? ->
            compositeDisposable.add(disposable!!)
            progressLiveStatus.postValue(LOADING)
        }.subscribe({ response: LeaderboardResponse? ->
            if (response != null && response.status == 200) {
                progressLiveStatus.postValue(LOADED)
                callback.onResult(response.leaderboardList!!, params.key + limit)
            }
        }, { t: Throwable? ->
            Timber.e(t, "Fetching leaderboard statistics failed")
            progressLiveStatus.postValue(LOADING)
        }))
    }
}
