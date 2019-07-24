package fr.free.nrw.commons.mwapi;

import org.apache.http.impl.client.AbstractHttpClient;

import java.io.IOException;
import java.util.HashMap;

import in.yuvi.http.fluent.Http;

public class CustomMwApi {
    public class RequestBuilder {
        private HashMap<String, Object> params;
        private CustomMwApi api;

        RequestBuilder(CustomMwApi api) {
            params = new HashMap<>();
            this.api = api;
        }

        public RequestBuilder param(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public CustomApiResult get() throws IOException {
            return api.makeRequest("GET", params);
        }

        public CustomApiResult post() throws IOException {
            return api.makeRequest("POST", params);
        }
    }

    private AbstractHttpClient client;
    private String apiURL;

    public CustomMwApi(String apiURL, AbstractHttpClient client) {
        this.apiURL = apiURL;
        this.client = client;
    }

    public RequestBuilder action(String action) {
        RequestBuilder builder = new RequestBuilder(this);
        builder.param("action", action);
        return builder;
    }

    private CustomApiResult makeRequest(String method, HashMap<String, Object> params) throws IOException {
        Http.HttpRequestBuilder builder;
        if (method.equals("POST")) {
            builder = Http.post(apiURL);
        } else {
            builder = Http.get(apiURL);
        }
        builder.data(params);
        return CustomApiResult.fromRequestBuilder(apiURL, builder, client);
    }
}
;
