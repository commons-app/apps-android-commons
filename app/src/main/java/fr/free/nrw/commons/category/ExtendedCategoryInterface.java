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

    @GET("w/api.php?action=query&format=json&formatversion=2"
        + "&prop=pageimages&piprop=thumbnail&pithumbsize=70")
    Single<MwQueryResponse> getCategoryThumbnail(@Query("titles") String categoryName);

}
