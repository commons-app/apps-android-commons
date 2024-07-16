package fr.free.nrw.commons.contributions

import androidx.paging.PagedList.BoundaryCallback
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Class that extends PagedList.BoundaryCallback for contributions list It defines the action that
 * is triggered for various boundary conditions in the list
 */
class ContributionBoundaryCallback @Inject constructor(
    private val repository: ContributionsRepository,
    private val sessionManager: SessionManager,
    private val mediaClient: MediaClient,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioThreadScheduler: Scheduler
) : BoundaryCallback<Contribution>() {
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    var userName: String? = null


    /**
     * It is triggered when the list has no items User's Contributions are then fetched from the
     * network
     */
    override fun onZeroItemsLoaded() {
        if (sessionManager.userName != null) {
            mediaClient.resetUserNameContinuation(sessionManager.userName!!)
        }
        fetchContributions()
    }

    /**
     * It is triggered when the user scrolls to the top of the list
     * */
    override fun onItemAtFrontLoaded(itemAtFront: Contribution) {

    }

    /**
     * It is triggered when the user scrolls to the end of the list. User's Contributions are then
     * fetched from the network
     */
    override fun onItemAtEndLoaded(itemAtEnd: Contribution) {
        fetchContributions()
    }

    /**
     * Fetches contributions using the MediaWiki API
     */
    private fun fetchContributions() {
        if (sessionManager.userName != null) {
            userName?.let { userName ->
                mediaClient.getMediaListForUser(userName)
                    .map { mediaList ->
                        mediaList.map { media ->
                            Contribution(media = media, state = Contribution.STATE_COMPLETED)
                        }
                    }
                    .subscribeOn(ioThreadScheduler)
                    .subscribe(::saveContributionsToDB) { error: Throwable ->
                        Timber.e(
                            "Failed to fetch contributions: %s",
                            error.message
                        )
                    }
            }?.let {
                compositeDisposable.add(
                    it
                )
            }
        }else {
            compositeDisposable.clear()
        }
    }

    /**
     * Saves the contributions the the local DB
     */
    private fun saveContributionsToDB(contributions: List<Contribution>) {
        compositeDisposable.add(
            repository.save(contributions)
                .subscribeOn(ioThreadScheduler)
                .subscribe { longs: List<Long?>? ->
                    repository["last_fetch_timestamp"] = System.currentTimeMillis()
                }
        )
    }

    /**
     * Clean up
     */
    fun dispose() {
        compositeDisposable.dispose()
    }
}
