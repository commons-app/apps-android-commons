package fr.free.nrw.commons

import androidx.core.text.HtmlCompat
import fr.free.nrw.commons.media.PAGE_ID_PREFIX
import fr.free.nrw.commons.media.IdAndCaptions
import fr.free.nrw.commons.media.MediaClient
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
class MediaDataExtractor @Inject constructor(private val mediaClient: MediaClient) {

    fun fetchDepictionIdsAndLabels(media: Media) =
        mediaClient.getEntities(media.depictionIds)
            .map {
                it.entities()
                    .mapValues { entry -> entry.value.labels().mapValues { it.value.value() } }
            }
            .map { it.map { (key, value) -> IdAndCaptions(key, value) } }
            .onErrorReturn { emptyList() }

    fun checkDeletionRequestExists(media: Media) =
        mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/" + media.filename)

    fun fetchDiscussion(media: Media) =
        mediaClient.getPageHtml(media.filename!!.replace("File", "File talk"))
            .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            .onErrorReturn {
                Timber.d("Error occurred while fetching discussion")
                ""
            }

    fun refresh(media: Media): Single<Media> {
        return Single.ambArray(
            mediaClient.getMediaById(PAGE_ID_PREFIX + media.pageId)
                .onErrorResumeNext { Single.never() },
            mediaClient.getMediaSuppressingErrors(media.filename)
                .onErrorResumeNext { Single.never() }
        )

    }

    fun getHtmlOfPage(title: String) = mediaClient.getPageHtml(title);

    /**
     * Fetches wikitext from mediaClient
     */
    fun getCurrentWikiText(title: String) = mediaClient.getCurrentWikiText(title);
}
