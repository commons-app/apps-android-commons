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

    public static ActionBuilder post() {
        return new PostBuilder(okHttpClient, gsonParser, parsedApiEndpoint);
    }

    public static ActionBuilder get() {
        return new GetBuilder(okHttpClient, gsonParser, parsedApiEndpoint);
    }

    @SuppressWarnings("WeakerAccess")
    public interface ActionBuilder {
        ParameterBuilder action(String action);
    }

    @SuppressWarnings("WeakerAccess")
    public interface ParameterBuilder {
        ParameterBuilder param(String name, String value);

        ParameterBuilder param(String name, int value);

        ParameterBuilder param(String name, InputStreamDescriptor value);

        ParameterBuilder withListener(MediaWikiApi.ProgressListener listener);

        ApiResponse execute();
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
