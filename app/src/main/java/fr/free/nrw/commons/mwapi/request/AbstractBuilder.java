package fr.free.nrw.commons.mwapi.request;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

abstract class AbstractBuilder<T> implements RequestBuilder.ActionBuilder<T>, RequestBuilder.ParameterBuilder<T> {
    private final Gson gsonParser;
    private final Class<T> returnClass;
    protected Map<String, Object> params = new HashMap<>();
    protected MediaWikiApi.ProgressListener listener;
    OkHttpClient okHttpClient;
    HttpUrl parsedApiEndpoint;

    AbstractBuilder(OkHttpClient okHttpClient, Gson gsonParser, HttpUrl parsedApiEndpoint, Class<T> returnClass) {
        this.okHttpClient = okHttpClient;
        this.gsonParser = gsonParser;
        this.parsedApiEndpoint = parsedApiEndpoint;
        this.returnClass = returnClass;
    }

    @NonNull
    @Override
    public RequestBuilder.ParameterBuilder<T> action(String action) {
        params.put("format", "json");
        params.put("action", action);
        return this;
    }

    @NonNull
    @Override
    public RequestBuilder.ParameterBuilder<T> param(String name, String value) {
        params.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public RequestBuilder.ParameterBuilder<T> param(String name, int value) {
        params.put(name, "" + value);
        return this;
    }

    @NonNull
    @Override
    public RequestBuilder.ParameterBuilder<T> param(String name, RequestBuilder.InputStreamDescriptor value) {
        params.put(name, value);
        return this;
    }

    @NonNull
    @Override
    public RequestBuilder.ParameterBuilder<T> withListener(MediaWikiApi.ProgressListener listener) {
        this.listener = listener;
        return this;
    }

    @Nullable
    @Override
    public T execute() {
        try {
            Response response = getResponse();
            if (response.code() < 300) {
                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }
                String stream = body.string();
                if (BuildConfig.DEBUG) {
                    Log.d("MW", "Response: " + stream);
                }
                return gsonParser.fromJson(stream, returnClass);
            }
        } catch (Exception e) {
            Log.e("MW", "Failed to execute request", e);
        }
        return null;
    }

    @NonNull
    protected abstract Response getResponse() throws IOException;
}
