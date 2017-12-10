package fr.free.nrw.commons.upload;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Uses the OkHttp library to implement asynchronous calls to the Commons MediaWiki API to match
 * GPS coordinates with nearby Commons categories. Parses the results using GSON to obtain a list
 * of relevant categories.
 */
public class CategoryApi {

    private static Set<String> categorySet;
    private static List<String> categoryList;
    private final OkHttpClient okHttpClient;
    private final HttpUrl mwUrl;
    private final Gson gson;

    @Inject
    public CategoryApi(OkHttpClient okHttpClient, @Named("commons_mediawiki_url") HttpUrl mwUrl, Gson gson) {
        this.okHttpClient = okHttpClient;
        this.mwUrl = mwUrl;
        this.gson = gson;
        categorySet = new HashSet<>();
    }

    public static List<String> getGpsCat() {
        return categoryList;
    }

    public static void setGpsCat(List<String> cachedList) {
        categoryList = new ArrayList<>();
        categoryList.addAll(cachedList);
        Timber.d("Setting GPS cats from cache: %s", categoryList);
    }

    public void request(String coords) {
        String apiUrl = buildUrl(coords);
        Timber.d("URL: %s", apiUrl);

        Call call = okHttpClient.newCall(new Request.Builder().get().url(apiUrl).build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Timber.e(e);
                GpsCatExists.setGpsCatExists(false);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) {
                categoryList = new ArrayList<>();
                categorySet = new HashSet<>();
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }
                QueryResponse queryResponse = gson.fromJson(body.charStream(), QueryResponse.class);
                if (queryResponse != null && queryResponse.query != null && queryResponse.query.pages != null) {
                    for (Page page : queryResponse.query.pages) {
                        if (page.categories != null) {
                            for (Category category : page.categories) {
                                String categoryString = category.title.replace("Category:", "");
                                categorySet.add(categoryString);
                            }
                            categoryList = new ArrayList<>(categorySet);
                        }
                    }
                }
                GpsCatExists.setGpsCatExists(!categorySet.isEmpty());
            }
        });
    }

    /**
     * Builds URL with image coords for MediaWiki API calls
     * Example URL: https://commons.wikimedia.org/w/api.php?action=query&prop=categories|coordinates|pageprops&format=json&clshow=!hidden&coprop=type|name|dim|country|region|globe&codistancefrompoint=38.11386944444445|13.356263888888888&generator=geosearch&redirects=&ggscoord=38.11386944444445|1.356263888888888&ggsradius=100&ggslimit=10&ggsnamespace=6&ggsprop=type|name|dim|country|region|globe&ggsprimary=all&formatversion=2
     *
     * @param coords Coordinates to build query with
     * @return URL for API query
     */
    private String buildUrl(String coords) {
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
                .build().toString();
    }

    public static class GpsCatExists {
        private static boolean gpsCatExists;

        public static void setGpsCatExists(boolean gpsCat) {
            gpsCatExists = gpsCat;
        }

        public static boolean getGpsCatExists() {
            return gpsCatExists;
        }
    }

    private static class QueryResponse {
        public Query query;

        public QueryResponse() {
        }
    }

    private static class Query {
        public Page[] pages;

        public Query() {
            pages = new Page[0];
        }
    }

    private static class Page {
        public String title;
        public Category[] categories;
        public Category category;

        public Page() {
        }
    }

    private static class Category {
        public String title;

        public Category() {
        }
    }
}



