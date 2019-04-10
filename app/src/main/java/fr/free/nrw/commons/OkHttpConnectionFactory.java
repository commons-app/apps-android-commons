package fr.free.nrw.commons;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.okhttp.HttpStatusException;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public final class OkHttpConnectionFactory {
    private static final String CACHE_DIR_NAME = "okhttp-cache";
    private static final long NET_CACHE_SIZE = 64 * 1024 * 1024;
    @NonNull private static final Cache NET_CACHE = new Cache(new File(CommonsApplication.getInstance().getCacheDir(),
            CACHE_DIR_NAME), NET_CACHE_SIZE);

    @NonNull private static OkHttpClient CLIENT = createClient();

    @NonNull public static OkHttpClient getClient() {
        return CLIENT;
    }

    @NonNull
    private static OkHttpClient createClient() {
        return new OkHttpClient.Builder()
                .cookieJar(SharedPreferenceCookieManager.getInstance())
                .cache(NET_CACHE)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .addInterceptor(new UnsuccessfulResponseInterceptor())
                .addInterceptor(new CommonHeaderRequestInterceptor())
                .build();
    }

    private static class CommonHeaderRequestInterceptor implements Interceptor {
        @Override @NonNull public Response intercept(@NonNull Chain chain) throws IOException {
            Request request = chain.request().newBuilder()
                    .header("User-Agent", CommonsApplication.getInstance().getUserAgent())
                    .build();
            return chain.proceed(request);
        }
    }

    public static class UnsuccessfulResponseInterceptor implements Interceptor {
        @Override @NonNull public Response intercept(@NonNull Chain chain) throws IOException {
            Response rsp = chain.proceed(chain.request());
            if (rsp.isSuccessful()) {
                return rsp;
            }
            throw new HttpStatusException(rsp);
        }
    }

    private OkHttpConnectionFactory() {
    }
}
