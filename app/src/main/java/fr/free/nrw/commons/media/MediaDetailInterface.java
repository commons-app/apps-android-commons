package fr.free.nrw.commons.media;

import com.google.gson.JsonObject;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MediaDetailInterface {

    /**
     * Fetches caption using file name
     *
     * @param filename name of the file to be used for fetching captions
     * */

    //Please note that languages=en does not have an impact on the languages returned. All captions are returned for all languages.
    @GET("w/api.php?action=wbgetentities&props=labels&format=json&languagefallback=1&sites=commonswiki")
    Observable<MediaDetailResponse> fetchStructuredDataByFilename(@Query("languages") String language, @Query("titles") String filename);

    @GET("w/api.php?action=wbsearchentities&format=json&language=en")
    Observable<JsonObject> fetchLabelForWikidata(@Query("search") String entityId);
}
