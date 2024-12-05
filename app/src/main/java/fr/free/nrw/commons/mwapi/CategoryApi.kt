package fr.free.nrw.commons.mwapi

import com.google.gson.Gson
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.category.CATEGORY_PREFIX
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

/**
 * Uses the OkHttp library to implement calls to the Commons MediaWiki API to match GPS coordinates
 * with nearby Commons categories. Parses the results using GSON to obtain a list of relevant
 * categories.  Note: that caller is responsible for executing the request() method on a background
 * thread.
 */
class CategoryApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val apiUrl : HttpUrl by lazy { BuildConfig.WIKIMEDIA_API_HOST.toHttpUrlOrNull()!! }

    fun request(coords: String): Single<List<CategoryItem>> = Single.fromCallable {
        val apiUrl = buildUrl(coords)
        Timber.d("URL: %s", apiUrl.toString())

        val request: Request = Request.Builder().get().url(apiUrl).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body ?: return@fromCallable emptyList<CategoryItem>()

        val apiResponse = gson.fromJson(body.charStream(), MwQueryResponse::class.java)
        val categories: MutableSet<CategoryItem> = mutableSetOf()
        if (apiResponse?.query() != null && apiResponse.query()!!.pages() != null) {
            for (page in apiResponse.query()!!.pages()!!) {
                if (page.categories() != null) {
                    for (category in page.categories()!!) {
                        categories.add(
                            CategoryItem(
                                name = category.title().replace(CATEGORY_PREFIX, ""),
                                description = "",
                                thumbnail = "",
                                isSelected = false
                            )
                        )
                    }
                }
            }
        }
        ArrayList<CategoryItem>(categories)
    }

    /**
     * Builds URL with image coords for MediaWiki API calls
     * Example URL: https://commons.wikimedia.org/w/api.php?action=query&prop=categories|coordinates|pageprops&format=json&clshow=!hidden&coprop=type|name|dim|country|region|globe&codistancefrompoint=38.11386944444445|13.356263888888888&generator=geosearch&redirects=&ggscoord=38.11386944444445|1.356263888888888&ggsradius=100&ggslimit=10&ggsnamespace=6&ggsprop=type|name|dim|country|region|globe&ggsprimary=all&formatversion=2
     *
     * @param coords Coordinates to build query with
     * @return URL for API query
     */
    private fun buildUrl(coords: String): HttpUrl = apiUrl.newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("prop", "categories|coordinates|pageprops")
        .addQueryParameter("format", "json")
        .addQueryParameter("clshow", "!hidden")
        .addQueryParameter("coprop", "type|name|dim|country|region|globe")
        .addQueryParameter("codistancefrompoint", coords)
        .addQueryParameter("generator", "geosearch")
        .addQueryParameter("ggscoord", coords)
        .addQueryParameter("ggsradius", "10000")
        .addQueryParameter("ggslimit", "10")
        .addQueryParameter("ggsnamespace", "6")
        .addQueryParameter("ggsprop", "type|name|dim|country|region|globe")
        .addQueryParameter("ggsprimary", "all")
        .addQueryParameter("formatversion", "2")
        .build()
}



