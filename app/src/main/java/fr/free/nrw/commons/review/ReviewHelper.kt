package fr.free.nrw.commons.review

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage.Revision
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewHelper
    @Inject
    constructor(
        private val mediaClient: MediaClient,
        private val reviewInterface: ReviewInterface,
    ) {
        @JvmField @Inject
        var dao: ReviewDao? = null



    /**
     * Fetches recent changes from MediaWiki API
     * Calls the API to get the latest 50 changes
     * When more results are available, the query gets continued beyond this range
     *
     * @return
     */
        private fun getRecentChanges() =
            reviewInterface
                .getRecentChanges()
                .map { it.query()?.pages() }
                .map(MutableList<MwQueryPage>::shuffled)
                .flatMapIterable { changes: List<MwQueryPage>? -> changes }
                .filter { isChangeReviewable(it) }

    /**
     * Gets multiple random media items for review.
     * - Fetches recent changes and filters them
     * - Checks if files are nominated for deletion
     * - Filters out already reviewed images
     *
     * @param count Number of media items to fetch
     * @return Observable of Media items
     */
    fun getRandomMediaBatch(count: Int): Observable<Media> =
        getRecentChanges()
            .flatMapSingle(::getRandomMediaFromRecentChange)
            .filter { media ->
                !media.filename.isNullOrBlank() &&
                        !getReviewStatus(media.pageId)
            }
            .take(count.toLong())
            .onErrorResumeNext { error: Throwable ->
                Timber.e(error, "Error getting random media batch")
                Observable.empty()
            }



    /**
     * Gets a random file change for review.
     *
     * @return Random file change
     */
    fun getRandomMedia(): Single<Media> =
        getRandomMediaBatch(1)
            .firstOrError()



        /**
         * Returns a proper Media object if the file is not already nominated for deletion
         * Else it returns an empty Media object
         *
         * @param recentChange
         * @return
         */
        private fun getRandomMediaFromRecentChange(recentChange: MwQueryPage) =
            Single
                .just(recentChange)
                .flatMap { mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/${it.title()}") }
                .flatMap {
                    if (it) {
                        Single.error(Exception("${recentChange.title()} is deleted"))
                    } else {
                        mediaClient.getMedia(recentChange.title())
                    }
                }

        /**
         * Checks if the image exists in the reviewed images entity
         *
         * @param image
         * @return
         */
        fun getReviewStatus(image: String?): Boolean = dao?.isReviewedAlready(image) ?: false

        /**
         * Gets the first revision of the file from filename
         *
         * @param filename
         * @return
         */
        fun getFirstRevisionOfFile(filename: String?): Observable<Revision> =
            reviewInterface
                .getFirstRevisionOfFile(filename)
                .map {
                    it
                        .query()
                        ?.firstPage()
                        ?.revisions()
                        ?.get(0)
                }

        /**
         * Checks Whether Given File is used in any Wiki page or not
         * by calling api for given file
         *
         * @param filename
         * @return
         */
        fun checkFileUsage(filename: String?): Observable<Boolean> =
            reviewInterface
                .getGlobalUsageInfo(filename)
                .map { it.query()?.firstPage()?.checkWhetherFileIsUsedInWikis() }



    /**
     * Batch checks whether multiple files are being used in any wiki pages.
     * This method processes a list of filenames in parallel using RxJava Observables.
     *
     * @param filenames A list of filenames to check for usage
     * @return Observable emitting pairs of filename and usage status:
     *         - The String represents the filename
     *         - The Boolean indicates whether the file is used (true) or not (false)
     *         If an error occurs during processing, it will log the error and emit an empty Observable
     */
    fun checkFileUsageBatch(filenames: List<String>): Observable<Pair<String, Boolean>> =
        // Convert the list of filenames into an Observable stream
        Observable.fromIterable(filenames)
            // For each filename, check its usage and pair it with the result
            .flatMap { filename ->
                checkFileUsage(filename)
                    // Create a pair of the filename and its usage status
                    .map { isUsed -> Pair(filename, isUsed) }
            }
            // Handle any errors that occur during processing
            .onErrorResumeNext { error: Throwable ->
                // Log the error and continue with an empty Observable
                Timber.e(error, "Error checking file usage batch")
                Observable.empty()
            }

        /**
         * Checks if the change is reviewable or not.
         * - checks the type and revisionId of the change
         * - checks supported image extensions
         *
         * @param recentChange
         * @return
         */
        private fun isChangeReviewable(recentChange: MwQueryPage): Boolean {
            for (extension in imageExtensions) {
                if (recentChange.title().endsWith(extension)) {
                    return true
                }
            }
            return false
        }

        /**
         * Adds reviewed/skipped images to the database
         *
         * @param imageId
         */
        fun addViewedImagesToDB(imageId: String?) {
            Completable
                .fromAction { dao!!.insert(ReviewEntity(imageId)) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        // Inserted successfully
                        Timber.i("Image inserted successfully.")
                    },
                ) { throwable: Throwable? -> Timber.e("Image not inserted into the reviewed images database") }
        }

        companion object {
            private val imageExtensions = arrayOf(".jpg", ".jpeg", ".png")
            private const val MAX_RETRIES = 3
        }
    }
