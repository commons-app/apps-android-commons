package fr.free.nrw.commons.mwapi.request;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class GetBuilder extends AbstractBuilder {
    GetBuilder(OkHttpClient okHttpClient, Gson gsonParser, HttpUrl parsedApiEndpoint) {
        super(okHttpClient, gsonParser, parsedApiEndpoint);
    }

    @Override
    protected Response getResponse() throws IOException {
        return okHttpClient.newCall(
                new Request.Builder()
                        .url(buildGetRequest())
                        .get()
                        .build()
        ).execute();
    }

    private HttpUrl buildGetRequest() {
        HttpUrl.Builder builder = parsedApiEndpoint.newBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            builder.addQueryParameter(entry.getKey(), (String) entry.getValue());
        }
        return builder.build();
    }
}
