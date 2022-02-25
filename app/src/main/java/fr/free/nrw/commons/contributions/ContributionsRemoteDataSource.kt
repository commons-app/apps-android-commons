package fr.free.nrw.commons.contributions

import androidx.paging.ItemKeyedDataSource
import fr.free.nrw.commons.data.models.Contribution
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Data-Source which acts as mediator for contributions-data from the API
 */
class ContributionsRemoteDataSource @Inject constructor(
    private val mediaClient: MediaClient,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioThreadScheduler: Scheduler
) : ItemKeyedDataSource<Int, Contribution>() {
    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    var userName: String? = null

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Contribution>
    ) {
        fetchContributions(callback)
    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Contribution>
    ) {
        fetchContributions(callback)
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Contribution>
    ) {
    }

    override fun getKey(item: Contribution): Int {
        return item.pageId.hashCode()
    }


    /**
     * Fetches contributions using the MediaWiki API
     */
    private fun fetchContributions(callback: LoadCallback<Contribution>) {
        compositeDisposable.add(
            mediaClient.getMediaListForUser(userName!!)
                .map { mediaList ->
                    mediaList.map {
                        Contribution(media = it, state = Contribution.STATE_COMPLETED)
                    }
                }
                .subscribeOn(ioThreadScheduler)
                .subscribe({
                    callback.onResult(it)
                }) { error: Throwable ->
                    Timber.e(
                        "Failed to fetch contributions: %s",
                        error.message
                    )
                }
        )
    }

    fun dispose() {
        compositeDisposable.dispose()
    }
}