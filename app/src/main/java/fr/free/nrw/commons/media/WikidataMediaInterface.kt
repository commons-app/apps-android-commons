package fr.free.nrw.commons.media

import fr.free.nrw.commons.media.MediaInterface.Companion.MEDIA_PARAMS
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface for getting Wikidata images from production server
 */
interface WikidataMediaInterface {
    /**
     * Fetches list of images from a depiction entity
     * @param query depictionEntityId ex. "haswbstatement:P180=Q9394"
     * @param srlimit the number of items to fetch
     * @param sroffset number of depictions already fetched,
     * this is useful in implementing pagination
     * @return Single<MwQueryResponse>
    </MwQueryResponse> */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2" +  //Basic parameters
        "&generator=search&gsrnamespace=6$MEDIA_PARAMS"         //Search parameters
    )
    fun fetchImagesForDepictedItem(
        @Query("gsrsearch") query: String?,
        @Query("gsrlimit") srlimit: String?, @Query("gsroffset") sroffset: String?
    ): Single<MwQueryResponse>
}
