package fr.free.nrw.commons

import androidx.core.text.HtmlCompat
import fr.free.nrw.commons.depictions.Media.DepictedImagesFragment
import fr.free.nrw.commons.media.Depictions
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import io.reactivex.functions.Function5
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetch additional media data from the network that we don't store locally.
 *
 * This includes things like category lists and multilingual descriptions,
 * which are not intrinsic to the media and may change due to editing.
 */
@Singleton
class MediaDataExtractor @Inject constructor(private val mediaClient: MediaClient) {

    /**
     * Simplified method to extract all details required to show media details.
     * It fetches media object, deletion status, talk page and captions for the filename
     * @param filename for which the details are to be fetched
     * @return full Media object with all details including deletion status and talk page
     */
    fun fetchMediaDetails(filename: String, pageId: String?): Single<Media> {
        return Single.zip(
            getMediaFromFileName(filename),
            mediaClient.checkPageExistsUsingTitle("Commons:Deletion_requests/$filename"),
            getDiscussion(filename),
            if (pageId != null)
                getCaption(DepictedImagesFragment.PAGE_ID_PREFIX + pageId)
            else Single.just(MediaClient.NO_CAPTION),
            getDepictions(filename),
            Function5 { media: Media, deletionStatus: Boolean, discussion: String, caption: String, depictions: Depictions ->
                combineToMedia(
                    media,
                    deletionStatus,
                    discussion,
                    caption,
                    depictions
                )
            }
        )
    }

    private fun combineToMedia(
        media: Media,
        deletionStatus: Boolean,
        discussion: String,
        caption: String,
        depictions: Depictions
    ): Media {
        media.discussion = discussion
        media.caption = caption
        media.depictions = depictions
        if (deletionStatus) {
            media.isRequestedDeletion = true
        }
        return media
    }

    /**
     * Obtains captions using filename
     * @param wikibaseIdentifier
     *
     * @return caption for the image in user's locale
     * Ex: "a nice painting" (english locale) and "No Caption" in case the caption is not available for the image
     */
    private fun getCaption(wikibaseIdentifier: String): Single<String> {
        return mediaClient.getCaptionByWikibaseIdentifier(wikibaseIdentifier)
    }

    /**
     * Fetch depictions from the MediaWiki API
     * @param filename the filename we will return the caption for
     * @return Depictions
     */
    private fun getDepictions(filename: String): Single<Depictions> {
        return mediaClient.getDepictions(filename)
            .doOnError { throwable: Throwable? ->
                Timber.e(
                    throwable,
                    "error while fetching depictions"
                )
            }
    }

    /**
     * Method can be used to fetch media for a given filename
     * @param filename Eg. File:Test.jpg
     * @return return data rich Media object
     */
    fun getMediaFromFileName(filename: String?): Single<Media> {
        return mediaClient.getMedia(filename)
    }

    /**
     * Fetch talk page from the MediaWiki API
     * @param filename
     * @return
     */
    private fun getDiscussion(filename: String): Single<String> {
        return mediaClient.getPageHtml(filename.replace("File", "File talk"))
            .map { discussion: String? ->
                HtmlCompat.fromHtml(
                    discussion!!,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ).toString()
            }
            .onErrorReturn { throwable: Throwable? ->
                Timber.e(throwable, "Error occurred while fetching discussion")
                ""
            }
    }

}
