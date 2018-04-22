package fr.free.nrw.commons.mwapi.request;

import android.os.Build;

import java.net.CookieManager;

import fr.free.nrw.commons.BuildConfig;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpClientFactory {
    private static final String USER_AGENT = "Commons/"
            + BuildConfig.VERSION_NAME
            + " (https://mediawiki.org/wiki/Apps/Commons) Android/"
            + Build.VERSION.RELEASE;

    public static OkHttpClient create(CookieManager cookieHandler) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain ->
                        chain.proceed(chain.request()
                                .newBuilder()
                                .header("User-Agent", USER_AGENT).build()))
                .cookieJar(new JavaNetCookieJar(cookieHandler));
        httpClientBuilder.socketFactory(new RestrictedSocketFactory(16 * 1024));

        // Only enable logging when it's a debug build.
        if (BuildConfig.DEBUG) {
            httpClientBuilder.addNetworkInterceptor(new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        return httpClientBuilder.build();
    }
}
