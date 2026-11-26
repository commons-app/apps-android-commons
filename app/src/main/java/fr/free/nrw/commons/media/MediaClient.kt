package fr.free.nrw.commons.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.utils.CommonsDateUtil
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.model.gallery.MediaInfo
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

const val PAGE_ID_PREFIX = "M"

/**
 * Media Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
class MediaClient
    @Inject
    constructor(
        private val mediaInterface: MediaInterface,
        private val pageMediaInterface: PageMediaInterface,
        private val mediaDetailInterface: MediaDetailInterface,
        private val responseMapper: MediaResponseMapper,
    ) {
        fun getMediaById(id: String) = responseMapper.map(mediaInterface.getMediaById(id)).map { it.first() }

        /**
         * Checks if a page exists on Commons
         * The same method can be used to check for file or talk page
         *
         * @param title File:Test.jpg or Commons:Deletion_requests/File:Test1.jpeg
         */
        fun checkPageExistsUsingTitle(title: String?): Single<Boolean> =
            mediaInterface
                .checkPageExistsUsingTitle(title)
                .map { it.query()!!.firstPage()!!.pageId() > 0 }

        /**
         * Take the fileSha and returns whether a file with a matching SHA exists or not
         *
         * @param fileSha SHA of the file to be checked
         */
        fun checkFileExistsUsingSha(fileSha: String?): Single<Boolean> =
            mediaInterface
                .checkFileExistsUsingSha(fileSha)
                .map { it.query()!!.allImages().size > 0 }

        /**
         * Fetches Media object from the imageInfo API
         *
         * @param titles the tiles to be searched for. Can be filename or template name
         * @return
         */
        fun getMedia(titles: String?): Single<Media> =
            responseMapper
                .map(mediaInterface.getMedia(titles))
                .map { it.first() }

        /**
         * Fetches Media object from the imageInfo API but suppress (known) errors
         *
         * @param titles the tiles to be searched for. Can be filename or template name
         * @return
         */
        fun getMediaSuppressingErrors(titles: String?): Single<Media> =
            responseMapper
                .map(mediaInterface.getMediaSuppressingErrors(titles))
                .map { it.first() }

        /**
         * Fetches playback sources and subtitle tracks from the videoInfo API.
         *
         * @param title the title of the media file whose info is to be fetched
         * @return a single that emits the media info for the requested file
         */
        fun getMediaInfo(title: String): Single<MediaInfo> =
            mediaInterface
                .getMediaInfo(title)
                .map {
                    it.query()?.firstPage()?.mediaInfo()
                        ?: error("No videoinfo returned for $title")
                }

        /**
         * The method returns the picture of the day
         *
         * @return Media object corresponding to the picture of the day
         */
        fun getPictureOfTheDay(): Single<Media> {
            val date = CommonsDateUtil.getIso8601DateFormatShort().format(Date())
            return responseMapper.map(mediaInterface.getMediaWithGenerator("Template:Potd/$date")).map { it.first() }
        }

        fun getPageHtml(title: String?): Single<String> =
            mediaInterface
                .getPageHtml(title)
                .map { obj: MwParseResponse -> obj.parse()?.text() ?: "" }

        fun getEntities(entityIds: List<String>): Single<Entities> = responseMapper.getEntities(entityIds)

        fun doesPageContainMedia(title: String?): Single<Boolean> =
            pageMediaInterface
                .getMediaList(title)
                .map { it.items.isNotEmpty() }

        /**
         * Get whole WikiText of required file
         * @param title : Name of the file
         * @return Observable<MwQueryResult>
         */
        fun getCurrentWikiText(title: String): Single<String?> =
            mediaDetailInterface.getWikiText(title).map {
                it
                    .query()
                    ?.pages()
                    ?.get(0)
                    ?.revisions()
                    ?.get(0)
                    ?.content()
                    .orEmpty()
            }
    }
