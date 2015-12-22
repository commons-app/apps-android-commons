package fr.free.nrw.commons.upload;

import android.content.Context;
import android.util.Log;

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


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

public class APICalls {
    
    private static RequestQueue REQUEST_QUEUE;
    private static final Gson GSON = new Gson();
    private Context context;

    public APICalls(Context context) {
        this.context = context;
    }
    public void request(String apiUrl) {
        JsonRequest<QueryResponse> request = new QueryRequest(apiUrl,
                new LogResponseListener<QueryResponse>(), new LogResponseErrorListener());
        getQueue().add(request);
    }

    private RequestQueue getQueue() {
        return getQueue(context);
    }

    private static RequestQueue getQueue(Context context) {
        if (REQUEST_QUEUE == null) {
            REQUEST_QUEUE = Volley.newRequestQueue(context.getApplicationContext());
        }
        return REQUEST_QUEUE;
    }

    private static class LogResponseListener<T> implements Response.Listener<T> {
        private static final String TAG = LogResponseListener.class.getName();

        @Override
        public void onResponse(T response) {
            Log.d(TAG, response.toString());
        }
    }

    private static class LogResponseErrorListener implements Response.ErrorListener {
        private static final String TAG = LogResponseErrorListener.class.getName();

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.toString());
        }
    }

    private static class QueryRequest extends JsonRequest<QueryResponse> {
        private static final String TAG = QueryRequest.class.getName();

        public QueryRequest(String url,
                            Response.Listener<QueryResponse> listener,
                            Response.ErrorListener errorListener) {
            super(Request.Method.GET, url, null, listener, errorListener);
        }

        @Override
        protected Response<QueryResponse> parseNetworkResponse(NetworkResponse response) {
            String json = parseString(response);
            //Log.d(TAG, "json=" + json);
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

    private static class QueryResponse {
        private Query query;

        @Override
        public String toString() {
            return "query=" + query.toString();
        }
    }

    private static class Query {
        private Page [] pages;

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("pages=");
            for (Page page : pages) {
                builder.append(page.toString());
                builder.append("\n");
                //page.extractCategories();
            }
            builder.replace(builder.length() - 1, builder.length(), "");

            return builder.toString();
        }
    }

    private static class Page {
        private int pageid;
        private int ns;
        private String title;
        //private ArrayList<Object> categories;
        //private Category category;

        /*
        public boolean categoryExists()  {
            if (categories != null) {
                return true;
            }
            else {
                return false;
            }
        }

        public void extractCategories() {
            if (categoryExists()==true) {
                Log.d("Cat", "categories exist");
                for (Object categoryobj : categories) {
                    Log.d("Cat", categoryobj.getClass().getName());
                }
            }
            else {
                Log.d("Cat", "categories don't exist");
            }
        }*/


        @Override
        public String toString() {

            //if (categoryExists()==true) {
                return "pageid=" + pageid + " ns=" + ns + " title=" + title + " categories=";// + category.toString();
            //}
            //else {
            //    return "pageid=" + pageid + " ns=" + ns + " title=" + title;
           // }
        }

    }
/*
    private class Category {
        private Category [] categories;
        private int ns;
        private String title;

        public Category(Object categoryobj) {
            //TODO
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("categories=");
            for (Category category: categories) {
                builder.append(category.toString());
                builder.append("\n");
            }
            builder.replace(builder.length() - 1, builder.length(), "");
            return builder.toString();
        }


    }*/
}

