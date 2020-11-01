package fr.free.nrw.commons;

import androidx.annotation.NonNull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import timber.log.Timber;

public final class OkHttpConnectionFactory {
    private static final String CACHE_DIR_NAME = "okhttp-cache";
    private static final long NET_CACHE_SIZE = 64 * 1024 * 1024;
    @NonNull private static final Cache NET_CACHE = new Cache(new File(CommonsApplication.getInstance().getCacheDir(),
            CACHE_DIR_NAME), NET_CACHE_SIZE);

    @NonNull
    private static final OkHttpClient CLIENT = createClient();

    @NonNull public static OkHttpClient getClient() {
        return CLIENT;
    }

    @NonNull
    private static OkHttpClient createClient() {
        return new OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.getInstance())
                .cache(NET_CACHE)
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(getLoggingInterceptor())
                .addInterceptor(new UnsuccessfulResponseInterceptor())
                .addInterceptor(new CommonHeaderRequestInterceptor())
                .build();
    }

    private static HttpLoggingInterceptor getLoggingInterceptor() {
        final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(Level.BODY);

        httpLoggingInterceptor.redactHeader("Authorization");
        httpLoggingInterceptor.redactHeader("Cookie");

        return httpLoggingInterceptor;
    }

    private static class CommonHeaderRequestInterceptor implements Interceptor {

        @Override
        @NonNull
        public Response intercept(@NonNull final Chain chain) throws IOException {
            final Request request = chain.request().newBuilder()
                    .header("User-Agent", CommonsApplication.getInstance().getUserAgent())
                    .build();
            return chain.proceed(request);
        }
    }

    public static class UnsuccessfulResponseInterceptor implements Interceptor {
        private static final List<String> DO_NOT_INTERCEPT = Collections.singletonList(
            "api.php?format=json&formatversion=2&errorformat=plaintext&action=upload&ignorewarnings=1");

        private static final String ERRORS_PREFIX = "{\"error";

        @Override
        @NonNull
        public Response intercept(@NonNull final Chain chain) throws IOException {
            final Response rsp = chain.proceed(chain.request());

            // Do not intercept certain requests and let the caller handle the errors
            if(isExcludedUrl(chain.request())) {
                return rsp;
            }
            if (rsp.isSuccessful()) {
                try (final ResponseBody responseBody = rsp.peekBody(ERRORS_PREFIX.length())) {
                    if (ERRORS_PREFIX.equals(responseBody.string())) {
                        try (final ResponseBody body = rsp.body()) {
                            throw new IOException(body.string());
                        }
                    }
                } catch (final IOException e) {
                    Timber.e(e);
                }
                return rsp;
            }
            throw new HttpStatusException(rsp);
        }

        private boolean isExcludedUrl(final Request request) {
            final String requestUrl = request.url().toString();
            for(final String url: DO_NOT_INTERCEPT) {
                if(requestUrl.contains(url)) {
                    return true;
                }
            }
            return false;
        }
    }

    private OkHttpConnectionFactory() {
    }
}
