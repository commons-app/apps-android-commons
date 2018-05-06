package fr.free.nrw.commons.mwapi.request;

import com.google.gson.Gson;

import java.io.InputStream;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.response.ApiResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class RequestBuilder {
    private static OkHttpClient okHttpClient;
    private static Gson gsonParser;
    private static HttpUrl parsedApiEndpoint;

    private RequestBuilder() {
    }

    public static void use(OkHttpClient httpClient, Gson gson, String apiHost) {
        okHttpClient = httpClient;
        gsonParser = gson;
        parsedApiEndpoint = HttpUrl.parse(apiHost);
    }

    public static <T> ActionBuilder<T> post(Class<T> returnClass) {
        return new PostBuilder<>(okHttpClient, gsonParser, parsedApiEndpoint, returnClass);
    }

    public static <T> ActionBuilder<T> get(Class<T> returnClass) {
        return new GetBuilder<T>(okHttpClient, gsonParser, parsedApiEndpoint, returnClass);
    }

    /** Convenience method - functionally equivalent to <code>post(..., ApiResponse.class)</code> */
    public static ActionBuilder<ApiResponse> post() {
        return new PostBuilder<>(okHttpClient, gsonParser, parsedApiEndpoint, ApiResponse.class);
    }

    /** Convenience method - functionally equivalent to <code>get(..., ApiResponse.class)</code> */
    public static ActionBuilder<ApiResponse> get() {
        return new GetBuilder<>(okHttpClient, gsonParser, parsedApiEndpoint, ApiResponse.class);
    }

    @SuppressWarnings("WeakerAccess")
    public interface ActionBuilder<T> {
        ParameterBuilder<T> action(String action);
    }

    @SuppressWarnings("WeakerAccess")
    public interface ParameterBuilder<T> {
        ParameterBuilder<T> param(String name, String value);

        ParameterBuilder<T> param(String name, int value);

        ParameterBuilder<T> param(String name, InputStreamDescriptor value);

        ParameterBuilder<T> withListener(MediaWikiApi.ProgressListener listener);

        T execute();
    }

    public static class InputStreamDescriptor {
        public final String filename;
        public final String mediaType;
        public final InputStream inputStream;
        public final long totalSize;

        public InputStreamDescriptor(String filename, String mediaType, InputStream inputStream, long totalSize) {
            this.filename = filename;
            this.mediaType = mediaType;
            this.inputStream = inputStream;
            this.totalSize = totalSize;
        }
    }
}
