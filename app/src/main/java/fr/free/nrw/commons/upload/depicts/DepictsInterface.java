package fr.free.nrw.commons.upload.depicts;

import fr.free.nrw.commons.depictions.models.DepictionResponse;
import fr.free.nrw.commons.wikidata.model.DepictSearchResponse;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Manges retrofit calls for Searching of depicts from DepictsFragment
 */

public interface DepictsInterface {

    @GET("/w/api.php?action=wbsearchentities&format=json&type=item&language=en&uselang=en")
    Observable<DepictSearchResponse> searchForDepicts(@Query("search") String query, @Query("limit") String limit);

    @GET("w/api.php?action=query&list=search&format=json&srnamespace=6")
    Observable<DepictionResponse> fetchListofDepictions(@Query("srsearch") String query);
}
