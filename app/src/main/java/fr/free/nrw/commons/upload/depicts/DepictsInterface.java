package fr.free.nrw.commons.upload.depicts;

import com.google.gson.JsonObject;

import fr.free.nrw.commons.depictions.models.DepictionResponse;
import fr.free.nrw.commons.wikidata.model.DepictSearchResponse;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Manges retrofit calls for Searching of depicts from DepictsFragment
 */

public interface DepictsInterface {

    /**
     * Search for depictions using the wbsearchentities API
     * @param query search for depictions based on user query
     * @param limit number of depictions to be retrieved
     * @param language current locale of the phone
     * @param uselang current locale of the phone
     * @param offset number of depictions already fetched useful in implementing pagination
     */
    @GET("/w/api.php?action=wbsearchentities&format=json&type=item&uselang=en")
    Observable<DepictSearchResponse> searchForDepicts(@Query("search") String query, @Query("limit") String limit, @Query("language") String language, @Query("uselang") String uselang, @Query("continue") String offset);

    @GET("/w/api.php?action=wbgetclaims&format=json&property=P18")
    Observable<JsonObject> getImageForEntity(@Query("entity") String entityId);
}
