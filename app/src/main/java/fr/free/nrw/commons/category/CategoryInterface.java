package fr.free.nrw.commons.category;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Interface for interacting with Commons category related APIs
 */
public interface CategoryInterface {

    /**
     * Searches for categories with the specified name.
     * Replaces ApacheHttpClientMediaWikiApi#allCategories
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=search&gsrnamespace=14")
    Observable<MwQueryResponse> searchCategories(@Query("gsrsearch") String filter,
                                                 @Query("gsrlimit") int itemLimit, @Query("gsroffset") int offset);

    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=allcategories")
    Observable<MwQueryResponse> searchCategoriesForPrefix(@Query("gacprefix") String prefix,
                                                          @Query("gaclimit") int itemLimit, @Query("gacoffset") int offset);
}
