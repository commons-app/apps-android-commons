package fr.free.nrw.commons.media;

import static fr.free.nrw.commons.media.MediaInterface.MEDIA_PARAMS;

import io.reactivex.Single;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for getting Wikidata images from production server
 */
public interface WikidataMediaInterface {

    /**
     * Fetches list of images from a depiction entity
     * @param query depictionEntityId ex. haswbstatement:P180=Q9394
     * @param srlimit the number of items to fetch
     * @param sroffset number of depictions already fetched,
     *                this is useful in implementing pagination
     * @return Single<MwQueryResponse>
     */
    @GET("w/api.php?action=query&format=json&formatversion=2" + //Basic parameters
        "&generator=search&gsrnamespace=6" + //Search parameters
        MEDIA_PARAMS)
    Single<MwQueryResponse> fetchImagesForDepictedItem(@Query("gsrsearch") String query,
        @Query("gsrlimit")String srlimit, @Query("gsroffset") String sroffset);

}
