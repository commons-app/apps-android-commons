package fr.free.nrw.commons.di;

import android.content.Context;

import com.google.gson.Gson;

import org.wikipedia.json.GsonUtil;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import timber.log.Timber;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class NetworkingModule {
    private static final String WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql";
    private static final String TOOLS_FORGE_URL = "https://tools.wmflabs.org/urbanecmbot/commonsmisc";

    private static final String TEST_TOOLS_FORGE_URL = "https://tools.wmflabs.org/commons-android-app/tool-commons-android-app";

    public static final long OK_HTTP_CACHE_SIZE = 10 * 1024 * 1024;

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Context context,
                                            HttpLoggingInterceptor httpLoggingInterceptor) {
        File dir = new File(context.getCacheDir(), "okHttpCache");
        return new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
            .readTimeout(60, TimeUnit.SECONDS)
            .cache(new Cache(dir, OK_HTTP_CACHE_SIZE))
            .build();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(message -> {
            Timber.tag("OkHttp").v(message);
        });
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return httpLoggingInterceptor;
    }

    @Provides
    @Singleton
    public MediaWikiApi provideMediaWikiApi(Context context,
                                            @Named("default_preferences") JsonKvStore defaultKvStore,
                                            Gson gson) {
        return new ApacheHttpClientMediaWikiApi(context, BuildConfig.WIKIMEDIA_API_HOST, BuildConfig.WIKIDATA_API_HOST, defaultKvStore, gson);
    }

    @Provides
    @Singleton
    public OkHttpJsonApiClient provideOkHttpJsonApiClient(OkHttpClient okHttpClient,
                                                          @Named("tools_force") HttpUrl toolsForgeUrl,
                                                          @Named("default_preferences") JsonKvStore defaultKvStore,
                                                          Gson gson) {
        return new OkHttpJsonApiClient(okHttpClient,
                toolsForgeUrl,
                WIKIDATA_SPARQL_QUERY_URL,
                BuildConfig.WIKIMEDIA_CAMPAIGNS_URL,
                BuildConfig.WIKIMEDIA_API_HOST,
                defaultKvStore,
                gson);
    }

    @Provides
    @Named("commons_mediawiki_url")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public HttpUrl provideMwUrl() {
        return HttpUrl.parse(BuildConfig.COMMONS_URL);
    }

    @Provides
    @Named("tools_force")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public HttpUrl provideToolsForgeUrl() {
        return HttpUrl.parse(TOOLS_FORGE_URL);
    }

    /**
     * Gson objects are very heavy. The app should ideally be using just one instance of it instead of creating new instances everywhere.
     * @return returns a singleton Gson instance
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return GsonUtil.getDefaultGson();
    }

}
