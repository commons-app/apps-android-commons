package fr.free.nrw.commons.review

import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage.Revision
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewHelper @Inject constructor(
    private val mediaClient: MediaClient,
    private val reviewInterface: ReviewInterface
) {
    @JvmField @Inject var dao: ReviewDao? = null

    /**
     * Fetches recent changes from MediaWiki API
     * Calls the API to get the latest 50 changes
     * When more results are available, the query gets continued beyond this range
     *
     * @return
     */
    private fun getRecentChanges() = reviewInterface.getRecentChanges()
        .map { it.query()?.pages() }
        .map(MutableList<MwQueryPage>::shuffled)
        .flatMapIterable { changes: List<MwQueryPage>? -> changes }
        .filter { isChangeReviewable(it) }

    /**
     * Gets a random file change for review.  Checks if the image has already been shown to the user
     * - Picks a random file from those changes
     * - Checks if the file is nominated for deletion
     * - Retries upto 5 times for getting a file which is not nominated for deletion
     *
     * @return Random file change
     */
    fun getRandomMedia(): Single<Media> = getRecentChanges()
        .flatMapSingle(::getRandomMediaFromRecentChange)
        .filter { !it.filename.isNullOrBlank() && !getReviewStatus(it.pageId) }
        .firstOrError()

    /**
     * Returns a proper Media object if the file is not already nominated for deletion
     * Else it returns an empty Media object
     *
     * @param recentChange
     * @return
     */
    private fun getRandomMediaFromRecentChange(recentChange: MwQueryPage) =
        Single.just(recentChange)
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
    fun getReviewStatus(image: String?): Boolean =
        dao?.isReviewedAlready(image) ?: false

    /**
     * Gets the first revision of the file from filename
     *
     * @param filename
     * @return
     */
    fun getFirstRevisionOfFile(filename: String?): Observable<Revision> =
        reviewInterface.getFirstRevisionOfFile(filename)
            .map { it.query()?.firstPage()?.revisions()?.get(0) }

    /**
     * Checks Whether Given File is used in any Wiki page or not
     * by calling api for given file
     *
     * @param filename
     * @return
     */
    fun checkFileUsage(filename: String?): Observable<Boolean> =
        reviewInterface.getGlobalUsageInfo(filename)
            .map { it.query()?.firstPage()?.checkWhetherFileIsUsedInWikis() }

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
        Completable.fromAction { dao!!.insert(ReviewEntity(imageId)) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    // Inserted successfully
                    Timber.i("Image inserted successfully.")
                }
            ) { throwable: Throwable? -> Timber.e("Image not inserted into the reviewed images database") }
    }

    companion object {
        private val imageExtensions = arrayOf(".jpg", ".jpeg", ".png")
    }
}
