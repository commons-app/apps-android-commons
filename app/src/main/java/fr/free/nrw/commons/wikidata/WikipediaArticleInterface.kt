package fr.free.nrw.commons.wikidata

import com.google.gson.JsonObject
import fr.free.nrw.commons.wikidata.model.WikipediaArticleResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Url

/** API for reading Wikipedia page properties from any language edition. */
interface WikipediaArticleInterface {
    /** Reads page properties from the given Wikipedia API URL. */
    @GET
    fun getPageProps(
        @Url url: String,
    ): Single<WikipediaArticleResponse>

    /** Reads the Wikimedia site matrix that lists every Wikipedia language edition. */
    @GET
    fun getSiteMatrix(
        @Url url: String,
    ): Single<JsonObject>
}
