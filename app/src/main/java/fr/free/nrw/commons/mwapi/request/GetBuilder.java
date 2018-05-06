package fr.free.nrw.commons.mwapi.request;

import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class GetBuilder<T> extends AbstractBuilder<T> {
    GetBuilder(OkHttpClient okHttpClient, Gson gsonParser, HttpUrl parsedApiEndpoint, Class<T> returnClass) {
        super(okHttpClient, gsonParser, parsedApiEndpoint, returnClass);
    }

    @NonNull
    @Override
    protected Response getResponse() throws IOException {
        return okHttpClient.newCall(
                new Request.Builder()
                        .url(buildGetRequest())
                        .get()
                        .build()
        ).execute();
    }

    @NonNull
    private HttpUrl buildGetRequest() {
        HttpUrl.Builder builder = parsedApiEndpoint.newBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            builder.addQueryParameter(entry.getKey(), (String) entry.getValue());
        }
        return builder.build();
    }
}
