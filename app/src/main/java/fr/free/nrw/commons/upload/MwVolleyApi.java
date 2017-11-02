package fr.free.nrw.commons.upload;

import android.net.Uri;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.free.nrw.commons.CommonsApplication;
import timber.log.Timber;

/**
 * Uses the Volley library to implement asynchronous calls to the Commons MediaWiki API to match
 * GPS coordinates with nearby Commons categories. Parses the results using GSON to obtain a list
 * of relevant categories.
 */
public class MwVolleyApi {

    private static RequestQueue REQUEST_QUEUE;
    private static final Gson GSON = new GsonBuilder().create();

    protected static Set<String> categorySet;
    private static List<String> categoryList;

    private static final String MWURL = "https://commons.wikimedia.org/";

    public MwVolleyApi() {
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

        JsonRequest<QueryResponse> request = new QueryRequest(apiUrl,
                new LogResponseListener<>(), new LogResponseErrorListener());
        getQueue().add(request);
    }

    /**
     * Builds URL with image coords for MediaWiki API calls
     * Example URL: https://commons.wikimedia.org/w/api.php?action=query&prop=categories|coordinates|pageprops&format=json&clshow=!hidden&coprop=type|name|dim|country|region|globe&codistancefrompoint=38.11386944444445|13.356263888888888&generator=geosearch&redirects=&ggscoord=38.11386944444445|1.356263888888888&ggsradius=100&ggslimit=10&ggsnamespace=6&ggsprop=type|name|dim|country|region|globe&ggsprimary=all&formatversion=2
     * @param coords Coordinates to build query with
     * @return URL for API query
     */
    private String buildUrl(String coords) {

        Uri.Builder builder = Uri.parse(MWURL).buildUpon();

        builder.appendPath("w")
                .appendPath("api.php")
                .appendQueryParameter("action", "query")
                .appendQueryParameter("prop", "categories|coordinates|pageprops")
                .appendQueryParameter("format", "json")
                .appendQueryParameter("clshow", "!hidden")
                .appendQueryParameter("coprop", "type|name|dim|country|region|globe")
                .appendQueryParameter("codistancefrompoint", coords)
                .appendQueryParameter("generator", "geosearch")
                .appendQueryParameter("ggscoord", coords)
                .appendQueryParameter("ggsradius", "10000")
                .appendQueryParameter("ggslimit", "10")
                .appendQueryParameter("ggsnamespace", "6")
                .appendQueryParameter("ggsprop", "type|name|dim|country|region|globe")
                .appendQueryParameter("ggsprimary", "all")
                .appendQueryParameter("formatversion", "2");

        return builder.toString();
    }

    private synchronized RequestQueue getQueue() {
        if (REQUEST_QUEUE == null) {
            REQUEST_QUEUE = Volley.newRequestQueue(CommonsApplication.getInstance());
        }
        return REQUEST_QUEUE;
    }

    private static class LogResponseListener<T> implements Response.Listener<T> {

        @Override
        public void onResponse(T response) {
            Timber.d(response.toString());
        }
    }

    private static class LogResponseErrorListener implements Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            Timber.e(error.toString());
        }
    }

    private static class QueryRequest extends JsonRequest<QueryResponse> {

        public QueryRequest(String url,
                            Response.Listener<QueryResponse> listener,
                            Response.ErrorListener errorListener) {
            super(Request.Method.GET, url, null, listener, errorListener);
        }

        @Override
        protected Response<QueryResponse> parseNetworkResponse(NetworkResponse response) {
            String json = parseString(response);
            QueryResponse queryResponse = GSON.fromJson(json, QueryResponse.class);
            return Response.success(queryResponse, cacheEntry(response));
        }

        private Cache.Entry cacheEntry(NetworkResponse response) {
            return HttpHeaderParser.parseCacheHeaders(response);
        }

        private String parseString(NetworkResponse response) {
            try {
                return new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            } catch (UnsupportedEncodingException e) {
                return new String(response.data);
            }
        }
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
        private Query query = new Query();

        private String printSet() {
            if (categorySet == null || categorySet.isEmpty()) {
                GpsCatExists.setGpsCatExists(false);
                Timber.d("gpsCatExists=%b", GpsCatExists.getGpsCatExists());
                return "No collection of categories";
            } else {
                GpsCatExists.setGpsCatExists(true);
                Timber.d("gpsCatExists=%b", GpsCatExists.getGpsCatExists());
                return "CATEGORIES FOUND" + categorySet.toString();
            }

        }

        @Override
        public String toString() {
            if (query != null) {
                return "query=" + query.toString() + "\n" + printSet();
            } else {
                return "No pages found";
            }
        }
    }

    private static class Query {
        private Page [] pages;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("pages=" + "\n");
            if (pages != null) {
                for (Page page : pages) {
                    builder.append(page.toString());
                    builder.append("\n");
                }
                builder.replace(builder.length() - 1, builder.length(), "");
                return builder.toString();
            } else {
                return "No pages found";
            }
        }
    }

    public static class Page {
        private int pageid;
        private int ns;
        private String title;
        private Category[] categories;
        private Category category;

        public Page() {
        }

        @Override
        public String toString() {

            StringBuilder builder = new StringBuilder("PAGEID=" + pageid + " ns=" + ns + " title=" + title + "\n" + " CATEGORIES= ");

            if (categories == null || categories.length == 0) {
                builder.append("no categories exist\n");
            } else {
                for (Category category : categories) {
                    builder.append(category.toString());
                    builder.append("\n");
                    if (category != null) {
                        String categoryString = category.toString().replace("Category:", "");
                        categorySet.add(categoryString);
                    }
                }
            }

            categoryList = new ArrayList<>(categorySet);
            builder.replace(builder.length() - 1, builder.length(), "");
            return builder.toString();
        }
    }

    private static class Category {
        private String title;

        @Override
        public String toString() {
            return title;
        }
    }
}



