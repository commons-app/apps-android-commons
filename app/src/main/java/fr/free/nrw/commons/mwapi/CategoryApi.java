package fr.free.nrw.commons.mwapi;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.mwapi.model.ApiResponse;
import fr.free.nrw.commons.mwapi.model.Page;
import fr.free.nrw.commons.mwapi.model.PageCategory;
import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Uses the OkHttp library to implement calls to the Commons MediaWiki API to match GPS coordinates
 * with nearby Commons categories. Parses the results using GSON to obtain a list of relevant
 * categories.  Note: that caller is responsible for executing the request() method on a background
 * thread.
 */
public class CategoryApi {

    private final OkHttpClient okHttpClient;
    private final HttpUrl mwUrl;
    private final Gson gson;

    @Inject
    public CategoryApi(OkHttpClient okHttpClient, Gson gson,
                       @Named("commons_mediawiki_url") HttpUrl mwUrl) {
        this.okHttpClient = okHttpClient;
        this.mwUrl = mwUrl;
        this.gson = gson;
    }

    public Single<List<String>> request(String coords) {
        return Single.fromCallable(() -> {
            HttpUrl apiUrl = buildUrl(coords);
            Timber.d("URL: %s", apiUrl.toString());

            Request request = new Request.Builder().get().url(apiUrl).build();
            Response response = okHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return Collections.emptyList();
            }

            ApiResponse apiResponse = gson.fromJson(body.charStream(), ApiResponse.class);
            Set<String> categories = new LinkedHashSet<>();
            if (apiResponse != null && apiResponse.hasPages()) {
                for (Page page : apiResponse.query.pages) {
                    for (PageCategory category : page.getCategories()) {
                        categories.add(category.withoutPrefix());
                    }
                }
            }
            return new ArrayList<>(categories);
        });
    }

    /**
     * Builds URL with image coords for MediaWiki API calls
     * Example URL: https://commons.wikimedia.org/w/api.php?action=query&prop=categories|coordinates|pageprops&format=json&clshow=!hidden&coprop=type|name|dim|country|region|globe&codistancefrompoint=38.11386944444445|13.356263888888888&generator=geosearch&redirects=&ggscoord=38.11386944444445|1.356263888888888&ggsradius=100&ggslimit=10&ggsnamespace=6&ggsprop=type|name|dim|country|region|globe&ggsprimary=all&formatversion=2
     *
     * @param coords Coordinates to build query with
     * @return URL for API query
     */
    private HttpUrl buildUrl(String coords) {
        return mwUrl.newBuilder()
                .addPathSegment("w")
                .addPathSegment("api.php")
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
                .build();
    }

}



