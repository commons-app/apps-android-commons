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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;


public class MwVolley {

    private Context context;
    private String coords;
    private static final String MWURL = "https://commons.wikimedia.org/";
    private String apiUrl;
    private int radius = 100;
    private boolean gpsCatExists;

    protected Set<String> categorySet;

    public MwVolley(Context context) {
        this.context = context;
        this.coords = "";
        categorySet = new HashSet<String>();
        RequestQueue queue = VolleyRequestQueue.getInstance(context.getApplicationContext()).getRequestQueue();
    }

    public void setCoords(String coords) {
        this.coords = coords;
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
    protected String buildUrl(int ggsradius) {

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
        //Add request to RequestQueue
        VolleyRequestQueue.getInstance(context).addToRequestQueue(request);
    }

    private class QueryRequest extends JsonRequest<QueryResponse> {

        public QueryRequest(String url, Response.Listener<QueryResponse> listener, Response.ErrorListener errorListener) {
            super(Request.Method.GET, url, null, listener, errorListener);
        }

        @Override
        protected Response<QueryResponse> parseNetworkResponse(NetworkResponse response) {
            String json = parseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(QueryResponse.class, new QueryResponseInstanceCreator());
            gsonBuilder.registerTypeAdapter(Query.class, new QueryInstanceCreator());
            gsonBuilder.registerTypeAdapter(Page.class, new PageInstanceCreator());
            gsonBuilder.registerTypeAdapter(Category.class, new CategoryInstanceCreator());
            Gson gson = gsonBuilder.create();

            QueryResponse queryResponse = gson.fromJson(json, QueryResponse.class);
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

    class QueryResponseInstanceCreator implements InstanceCreator<QueryResponse> {
        public QueryResponse createInstance(Type type)
        {
            return new QueryResponse();
        }
    }

    class QueryInstanceCreator implements InstanceCreator<Query> {
        public Query createInstance(Type type)
        {
            return new Query();
        }
    }

    class PageInstanceCreator implements InstanceCreator<Page> {
        public Page createInstance(Type type)
        {
            return new Page();
        }
    }

    class CategoryInstanceCreator implements InstanceCreator<Category> {
        public Category createInstance(Type type)
        {
            return new Category();
        }
    }

    private class QueryResponse {

        private Query query;

        public QueryResponse() {
            this.query = new Query();

        }
        private String printSet() {

            if (categorySet == null || categorySet.isEmpty()) {
               setGpsCatExists(false);
                Log.d("Cat", "gpsCatExists=" + getGpsCatExists());
                return "No collection of categories";
            } else {
                setGpsCatExists(true);
                Log.d("Cat", "gpsCatExists=" + getGpsCatExists());
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

    public List<String> getGpsCat() {
        List<String> list = new ArrayList<String>(categorySet);
        return list;
    }

    public void setGpsCatExists(boolean gpsCat) {
        gpsCatExists = gpsCat;
    }

    public boolean getGpsCatExists() {
        return gpsCatExists;
    }

    private class Query {
        private Page[] pages;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("pages=" + "\n");
            for (Page page : pages) {
                builder.append(page.toString());
                builder.append("\n");
            }
            builder.replace(builder.length() - 1, builder.length(), "");

            return builder.toString();

        }
    }

    private class Page {
        private Category[] categories;
        private Category category;

        @Override
        public String toString() {

            StringBuilder builder = new StringBuilder(" CATEGORIES= ");

            if (categories == null || categories.length == 0) {
                builder.append("no categories exist\n");
            } else {
                for (Category category : categories) {
                    if (category != null) {
                        String categoryString = category.toString().replace("Category:", "");
                        categorySet.add(categoryString);
                    }
                    builder.append(category.toString());
                    builder.append(", ");
                }
            }

            builder.replace(builder.length() - 1, builder.length(), "");
            return builder.toString();
        }
    }

    private class Category {
        private String title;

        @Override
        public String toString() {
            return title;
        }
    }





}
