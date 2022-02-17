package fr.free.nrw.commons.category;

import io.reactivex.Single;
import java.util.Map;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Interface for interacting with Commons category related APIs
 */
public interface CategoryInterface {

    /**
     * Searches for categories with the specified name.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=search&prop=description|pageimages&piprop=thumbnail&pithumbsize=70"
            + "&gsrnamespace=14")
    Single<MwQueryResponse> searchCategories(@Query("gsrsearch") String filter,
                                                 @Query("gsrlimit") int itemLimit,
                                                 @Query("gsroffset") int offset);

    /**
     * Searches for categories starting with the specified prefix.
     *
     * @param prefix    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=allcategories&prop=categoryinfo|description|pageimages&piprop=thumbnail"
            + "&pithumbsize=70")
    Single<MwQueryResponse> searchCategoriesForPrefix(@Query("gacprefix") String prefix,
                                                          @Query("gaclimit") int itemLimit,
                                                          @Query("gacoffset") int offset);

    /**
     * Fetches categories starting and ending with a specified name.
     *
     * @param startingCategory Name of the category to start
     * @param endingCategory Name of the category to end
     * @param itemLimit How many categories to return
     * @param offset offset
     * @return MwQueryResponse
     */
    @GET("w/api.php?action=query&format=json&formatversion=2"
        + "&generator=allcategories&prop=categoryinfo|description|pageimages&piprop=thumbnail"
        + "&pithumbsize=70")
    Single<MwQueryResponse> getCategoriesByName(@Query("gacfrom") String startingCategory,
        @Query("gacto") String endingCategory,
        @Query("gaclimit") int itemLimit,
        @Query("gacoffset") int offset);

    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=categorymembers&gcmtype=subcat"
            + "&prop=info&gcmlimit=50")
    Single<MwQueryResponse> getSubCategoryList(@Query("gcmtitle") String categoryName,
        @QueryMap(encoded = true) Map<String, String> continuation);

    @GET("w/api.php?action=query&format=json&formatversion=2"
            + "&generator=categories&prop=info&gcllimit=50")
    Single<MwQueryResponse> getParentCategoryList(@Query("titles") String categoryName,
        @QueryMap(encoded = true) Map<String, String> continuation);

}
