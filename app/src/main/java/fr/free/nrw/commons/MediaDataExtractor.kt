package fr.free.nrw.commons

import androidx.core.text.HtmlCompat
import fr.free.nrw.commons.media.IdAndLabels
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.media.isFileDeletedOnCommons
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetch additional media data from the network that we don't store locally.
 *
 *
 * This includes things like category lists and multilingual descriptions, which are not intrinsic
 * to the media and may change due to editing.
 */
@Singleton
class MediaDataExtractor
    @Inject
    constructor(
        private val mediaClient: MediaClient,
    ) {
        fun fetchDepictionIdsAndLabels(media: Media) =
            mediaClient
                .getEntities(media.depictionIds)
                .map {
                    it
                        .getEntityMap()
                        .mapValues { entry -> entry.value.labels().mapValues { it.value.value() } }
                }.map { it.map { (key, value) -> IdAndLabels(key, value) } }
                .onErrorReturn { emptyList() }

        fun fetchCreatorIdsAndLabels(media: Media) =
            mediaClient
                .getEntities(media.creatorIds)
                .map {
                    it
                        .getEntityMap()
                        .mapValues { entry -> entry.value.labels().mapValues { it.value.value() } }
                }.map { it.map { (key, value) -> IdAndLabels(key, value) } }
                .onErrorReturn { emptyList() }

        fun checkDeletionRequestExists(media: Media) =
            mediaClient.checkPageExistsUsingTitle(
                "Commons:Deletion_requests/" + media.filename,
            )

        fun fetchDiscussion(media: Media) =
            mediaClient
                .getPageHtml(media.filename!!.replace("File", "File talk"))
                .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
                .onErrorReturn {
                    Timber.d("Error occurred while fetching discussion")
                    ""
                }

        fun refresh(media: Media): Single<Media> =
            Single.ambArray(
                mediaClient
                    .getMediaById(PAGE_ID_PREFIX + media.pageId)
                    .onErrorResumeNext(::keepGoingUnlessFileDeleted),
                mediaClient
                    .getMediaSuppressingErrors(media.filename)
                    .onErrorResumeNext(::keepGoingUnlessFileDeleted),
            )

        /**
         * The two lookups race. Anything other than a deleted file stays silent so the other
         * lookup can still win, and a network failure is not treated as a deletion.
         */
        private fun keepGoingUnlessFileDeleted(error: Throwable): Single<Media> =
            if (error.isFileDeletedOnCommons()) Single.error(error) else Single.never()

        fun getHtmlOfPage(title: String) = mediaClient.getPageHtml(title)

        /**
         * Fetches wikitext from mediaClient
         */
        fun getCurrentWikiText(title: String) = mediaClient.getCurrentWikiText(title).map { it.orEmpty() }
    }
