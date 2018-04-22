package fr.free.nrw.commons.mwapi.request;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.response.ApiResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

abstract class AbstractBuilder implements RequestBuilder.ActionBuilder, RequestBuilder.ParameterBuilder {
    protected Map<String, Object> params = new HashMap<>();
    protected MediaWikiApi.ProgressListener listener;
    private final Gson gsonParser;
    OkHttpClient okHttpClient;
    HttpUrl parsedApiEndpoint;

    AbstractBuilder(OkHttpClient okHttpClient, Gson gsonParser, HttpUrl parsedApiEndpoint) {
        this.okHttpClient = okHttpClient;
        this.gsonParser = gsonParser;
        this.parsedApiEndpoint = parsedApiEndpoint;
    }

    @Override
    public RequestBuilder.ParameterBuilder action(String action) {
        params.put("format", "json");
        params.put("action", action);
        return this;
    }

    @Override
    public RequestBuilder.ParameterBuilder param(String name, String value) {
        params.put(name, value);
        return this;
    }

    @Override
    public RequestBuilder.ParameterBuilder param(String name, int value) {
        params.put(name, "" + value);
        return this;
    }

    @Override
    public RequestBuilder.ParameterBuilder param(String name, RequestBuilder.InputStreamDescriptor value) {
        params.put(name, value);
        return this;
    }

    @Override
    public RequestBuilder.ParameterBuilder withListener(MediaWikiApi.ProgressListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public ApiResponse execute() {
        try {
            Response response = getResponse();
            if (response.code() < 300) {
                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }
                String stream = body.string();
                if (BuildConfig.DEBUG) {
                    Log.e("MW", "Response: " + stream);
                }
                return gsonParser.fromJson(stream, ApiResponse.class);
            }
        } catch (Exception e) {
            Log.e("MW", "Failed to execute request", e);
        }
        return null;
    }

    protected abstract Response getResponse() throws IOException;
}
