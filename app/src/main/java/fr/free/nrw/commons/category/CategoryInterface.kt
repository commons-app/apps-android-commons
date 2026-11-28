package fr.free.nrw.commons.category

import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Interface for interacting with Commons category related APIs
 */
interface CategoryInterface {
    /**
     * Searches for categories with the specified name.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=search&prop=description|" +
            "pageimages&piprop=thumbnail&pithumbsize=70&gsrnamespace=14",
    )
    fun searchCategories(
        @Query("gsrsearch") filter: String?,
        @Query("gsrlimit") itemLimit: Int,
        @Query("gsroffset") offset: Int,
    ): Single<MwQueryResponse>

    /**
     * Same search as [searchCategories], but also asks for categoryinfo so hidden categories
     * can be dropped. Used by the upload category search.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @param continuation The continuation of the previous page, empty for the first one
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=search&prop=categoryinfo|" +
            "description|pageimages&piprop=thumbnail&pithumbsize=70&gsrnamespace=14",
    )
    fun searchCategoriesWithInfo(
        @Query("gsrsearch") filter: String?,
        @Query("gsrlimit") itemLimit: Int,
        @QueryMap continuation: Map<String, String>,
    ): Single<MwQueryResponse>

    /**
     * Searches for categories starting with the specified prefix. Uses the search index behind
     * the Commons search box, which ignores case, so "supermarkets in tokyo" finds
     * "Supermarkets in Tokyo".
     *
     * @param prefix    The string to be searched
     * @param itemLimit How many results are returned
     * @param continuation The continuation of the previous page, empty for the first one
     * @return
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=prefixsearch&gpsnamespace=14&" +
            "prop=categoryinfo|description|pageimages&piprop=thumbnail&pithumbsize=70",
    )
    fun searchCategoriesForPrefix(
        @Query("gpssearch") prefix: String?,
        @Query("gpslimit") itemLimit: Int,
        @QueryMap continuation: Map<String, String>,
    ): Single<MwQueryResponse>

    /**
     * Fetches categories starting and ending with a specified name.
     *
     * @param startingCategory Name of the category to start
     * @param endingCategory Name of the category to end
     * @param itemLimit How many categories to return
     * @param offset offset
     * @return MwQueryResponse
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=allpages&gapnamespace=14&" +
            "prop=description|pageimages&piprop=thumbnail&pithumbsize=70",
    )
    fun getCategoriesByName(
        @Query("gapfrom") startingCategory: String?,
        @Query("gapto") endingCategory: String?,
        @Query("gaplimit") itemLimit: Int,
        @Query("gapoffset") offset: Int,
    ): Single<MwQueryResponse>

    /**
     * Fetches non-hidden categories by titles.
     *
     * @param titles titles to fetch categories for (e.g. File:<P18 of a wikidata entity>)
     * @param itemLimit How many categories to return
     * @return MwQueryResponse
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=categories&" +
            "prop=categoryinfo|description|pageimages&piprop=thumbnail&pithumbsize=70&gclshow=!hidden",
    )
    fun getCategoriesByTitles(
        @Query("titles") titles: String?,
        @Query("gcllimit") itemLimit: Int,
    ): Single<MwQueryResponse>

    @GET(
        "w/api.php?action=query&format=json&formatversion=2&generator=categorymembers&" +
            "gcmtype=subcat&prop=info&gcmlimit=50",
    )
    fun getSubCategoryList(
        @Query("gcmtitle") categoryName: String,
        @QueryMap(encoded = true) continuation: Map<String, String>,
    ): Single<MwQueryResponse>

    @GET("w/api.php?action=query&format=json&formatversion=2&generator=categories&prop=info&gcllimit=50")
    fun getParentCategoryList(
        @Query("titles") categoryName: String?,
        @QueryMap(encoded = true) continuation: Map<String, String>,
    ): Single<MwQueryResponse>

    /**
     * Checks whether categories are redirects. Hard redirects and normalized titles are listed in
     * the response, and a soft redirect page is listed as a member of [redirectCategory].
     *
     * @param titles up to 50 category titles separated by "|", such as "Category:Cats"
     * @param redirectCategory the category that lists soft category redirects
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&redirects=1&prop=categories&cllimit=max",
    )
    fun getCategoryRedirectInfo(
        @Query("titles") titles: String,
        @Query("clcategories") redirectCategory: String,
    ): Single<CategoryRedirectResponse>

    /**
     * Fetches the links to other categories of the given category pages. A soft category
     * redirect links only to the category it redirects to.
     *
     * @param titles up to 50 category titles separated by "|"
     */
    @GET(
        "w/api.php?action=query&format=json&formatversion=2&prop=links&plnamespace=14&pllimit=max",
    )
    fun getCategoryLinks(
        @Query("titles") titles: String,
    ): Single<CategoryRedirectResponse>
}
