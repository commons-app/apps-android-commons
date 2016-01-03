package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class MwVolley {

    private Context context;
    private String coords;
    private static final String MWURL = "https://commons.wikimedia.org/";
    private String apiUrl;
    private int radius = 100;
    private Gson GSON;

    protected Set<String> categorySet;

    public MwVolley(Context context, String coords) {

        this.context = context;
        this.coords = coords;
        categorySet = new HashSet<String>();
        GSON = new GsonBuilder().create();
        //Instantiate RequestQueue with Application context
        RequestQueue queue = VolleyRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
    }

    protected void setRadius(int radius) {
        this.radius = radius;
    }

    protected int getRadius() {
        return radius;
    }

    /**
     * Builds URL with image coords for MediaWiki API calls
     * Example URL: https://commons.wikimedia.org/w/api.php?action=query&prop=categories|coordinates|pageprops&format=json&clshow=!hidden&coprop=type|name|dim|country|region|globe&codistancefrompoint=38.11386944444445|13.356263888888888&
     * generator=geosearch&redirects=&ggscoord=38.11386944444445|13.356263888888888&ggsradius=100&ggslimit=10&ggsnamespace=6&ggsprop=type|name|dim|country|region|globe&ggsprimary=all&formatversion=2
     */
    private String buildUrl(int ggsradius) {

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
                .appendQueryParameter("ggsradius", Integer.toString(ggsradius))
                .appendQueryParameter("ggslimit", "10")
                .appendQueryParameter("ggsnamespace", "6")
                .appendQueryParameter("ggsprop", "type|name|dim|country|region|globe")
                .appendQueryParameter("ggsprimary", "all")
                .appendQueryParameter("formatversion", "2");

        return builder.toString();
    }

    public void request() {

        radius = getRadius();
        apiUrl = buildUrl(radius);

        ShareActivity.ResponseListener responseListener = new ShareActivity().new ResponseListener();
        ShareActivity.ErrorListener errorListener = new ShareActivity().new ErrorListener();

        JsonRequest request = new QueryRequest(apiUrl, responseListener, errorListener);
        VolleyRequestQueue.getInstance(context).addToRequestQueue(request);
    }

    private class QueryRequest extends JsonRequest<QueryResponse> {

        public QueryRequest(String url, Response.Listener<QueryResponse> listener, Response.ErrorListener errorListener) {
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

    private class QueryResponse {
        private Query query = new Query();

        private String printSet() {
            GpsCatExists gpsCatExists = new GpsCatExists();
            if (categorySet == null || categorySet.isEmpty()) {
                gpsCatExists.setGpsCatExists(false);
                Log.d("Cat", "gpsCatExists=" + gpsCatExists.getGpsCatExists());
                return "No collection of categories";
            } else {
                gpsCatExists.setGpsCatExists(true);
                Log.d("Cat", "gpsCatExists=" + gpsCatExists.getGpsCatExists());
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

    public class GpsCatExists {
        private boolean gpsCatExists;

        public void setGpsCatExists(boolean gpsCat) {
            gpsCatExists = gpsCat;
        }

        public boolean getGpsCatExists() {
            return gpsCatExists;
        }
    }




}
