package fr.free.nrw.commons.category;

import io.reactivex.Single;
import java.util.Map;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface for interacting with https://commons.wikimedia.org category related APIs
 */
public interface ExtendedCategoryInterface {

    /**
     * API call for getting thumbnail and description of a category
     * @param categoryName title
     * @return Single<MwQueryResponse>
     */
    @GET("w/api.php?action=query&format=json&formatversion=2"
        + "&prop=description|pageimages&piprop=thumbnail&pithumbsize=70")
    Single<MwQueryResponse> getCategoryInfo(@Query("titles") String categoryName);

}
