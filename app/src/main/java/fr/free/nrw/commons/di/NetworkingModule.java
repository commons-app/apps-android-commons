package fr.free.nrw.commons.di;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class NetworkingModule {
    public static final long OK_HTTP_CACHE_SIZE = 10 * 1024 * 1024;

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Context context) {
        File dir = new File(context.getCacheDir(), "okHttpCache");
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cache(new Cache(dir, OK_HTTP_CACHE_SIZE))
            .build();
    }

    @Provides
    @Singleton
    public MediaWikiApi provideMediaWikiApi(Context context,
                                            @Named("default_preferences") SharedPreferences defaultPreferences,
                                            @Named("category_prefs") SharedPreferences categoryPrefs,
                                            Gson gson,
                                            OkHttpClient okHttpClient) {
        return new ApacheHttpClientMediaWikiApi(context, BuildConfig.WIKIMEDIA_API_HOST, BuildConfig.WIKIDATA_API_HOST, defaultPreferences, categoryPrefs, gson, okHttpClient);
    }

    @Provides
    @Named("commons_mediawiki_url")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public HttpUrl provideMwUrl() {
        return HttpUrl.parse(BuildConfig.COMMONS_URL);
    }

    /**
     * Gson objects are very heavy. The app should ideally be using just one instance of it instead of creating new instances everywhere.
     * @return returns a singleton Gson instance
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

}
