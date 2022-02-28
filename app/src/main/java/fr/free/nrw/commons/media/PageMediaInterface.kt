package fr.free.nrw.commons.media

import fr.free.nrw.commons.data.models.media.PageMediaListResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface for MediaWiki Page REST APIs
 */
interface PageMediaInterface {
    /**
     * Get a list of media used on a page
     *
     * @param title the title of the page
     */
    @GET("api/rest_v1/page/media-list/{title}")
    fun getMediaList(@Path("title") title: String?): Single<PageMediaListResponse>
}
