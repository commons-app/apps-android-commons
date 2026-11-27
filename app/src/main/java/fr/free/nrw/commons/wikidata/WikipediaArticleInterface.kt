package fr.free.nrw.commons.wikidata

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
}
