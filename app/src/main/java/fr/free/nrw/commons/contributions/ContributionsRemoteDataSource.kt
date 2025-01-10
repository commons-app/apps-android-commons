package fr.free.nrw.commons.contributions

import androidx.paging.ItemKeyedDataSource
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.IO_THREAD
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Data-Source which acts as mediator for contributions-data from the API
 */
class ContributionsRemoteDataSource
    @Inject
    constructor(
        private val mediaClient: MediaClient,
        @param:Named(IO_THREAD) private val ioThreadScheduler: Scheduler,
    ) : ItemKeyedDataSource<Int, Contribution>() {
        private val compositeDisposable: CompositeDisposable = CompositeDisposable()
        var userName: String? = null

        override fun loadInitial(
            params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Contribution>,
        ) {
            fetchAllContributions(callback)
        }

        override fun loadAfter(
            params: LoadParams<Int>,
            callback: LoadCallback<Contribution>,
        ) {
            fetchAllContributions(callback)
        }

        override fun loadBefore(
            params: LoadParams<Int>,
            callback: LoadCallback<Contribution>,
        ) {
        }

        override fun getKey(item: Contribution): Int = item.pageId.hashCode()

        /**
         * Fetches contributions using the MediaWiki API
         */
        fun fetchAllContributions(callback: LoadCallback<Contribution>) {
            if (userName.isNullOrEmpty()) {
                Timber.e("Failed to fetch contributions: userName is null or empty")
                return
            }
            Timber.d("Fetching contributions for user: $userName")

            compositeDisposable.add(
                mediaClient
                    .getMediaListForUser(userName!!)
                    .map { mediaList ->
                        mediaList.map {
                            Contribution(media = it, state = Contribution.STATE_COMPLETED)
                        }
                    }.subscribeOn(ioThreadScheduler)
                    .subscribe({ contributions ->
                        // Pass the contributions to the callback
                        callback.onResult(contributions)
                    })  { error: Throwable ->
                        Timber.e(
                            "Failed to fetch contributions: %s",
                            error.message,
                        )
                    },
            )
        }
    /**
     * Fetches the latest contribution identifier only
     */
    fun fetchLatestContributionIdentifier(callback: (String?) -> Unit) {
        if (userName.isNullOrEmpty()) {
            Timber.e("Failed to fetch latest contribution: userName is null or empty")
            return
        }
        Timber.d("Fetching latest contribution identifier for user: $userName")

        compositeDisposable.add(
            mediaClient.getMediaListForUser(userName!!)
                .map { mediaList ->
                    mediaList.firstOrNull()?.pageId.toString() // Extract the first contribution's pageId
                }
                .subscribeOn(ioThreadScheduler)
                .subscribe({ latestIdentifier ->
                    callback(latestIdentifier)
                }) { error: Throwable ->
                    Timber.e("Failed to fetch latest contribution identifier: %s", error.message)
                    callback(null)
                },
        )
    }

        fun dispose() {
            compositeDisposable.dispose()
        }
    }
