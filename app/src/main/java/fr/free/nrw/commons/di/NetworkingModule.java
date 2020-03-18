package fr.free.nrw.commons.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.login.LoginClient;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.actions.PageEditInterface;
import fr.free.nrw.commons.category.CategoryInterface;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaInterface;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.mwapi.UserInterface;
import fr.free.nrw.commons.review.ReviewInterface;
import fr.free.nrw.commons.upload.UploadInterface;
import fr.free.nrw.commons.wikidata.WikidataInterface;
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

    public static final String NAMED_COMMONS_WIKI_SITE = "commons-wikisite";
    private static final String NAMED_WIKI_DATA_WIKI_SITE = "wikidata-wikisite";

    public static final String NAMED_COMMONS_CSRF = "commons-csrf";

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
    public OkHttpJsonApiClient provideOkHttpJsonApiClient(OkHttpClient okHttpClient,
                                                          @Named("tools_forge") HttpUrl toolsForgeUrl,
                                                          @Named("default_preferences") JsonKvStore defaultKvStore,
                                                          Gson gson) {
        return new OkHttpJsonApiClient(okHttpClient,
                toolsForgeUrl,
                WIKIDATA_SPARQL_QUERY_URL,
                BuildConfig.WIKIMEDIA_CAMPAIGNS_URL,
                BuildConfig.WIKIMEDIA_API_HOST,
                gson);
    }

    @Named(NAMED_COMMONS_CSRF)
    @Provides
    @Singleton
    public CsrfTokenClient provideCommonsCsrfTokenClient(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return new CsrfTokenClient(commonsWikiSite, commonsWikiSite);
    }

    @Provides
    @Singleton
    public LoginClient provideLoginClient() {
        return new LoginClient();
    }

    @Provides
    @Named("wikimedia_api_host")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public String provideMwApiUrl() {
        return BuildConfig.WIKIMEDIA_API_HOST;
    }

    @Provides
    @Named("tools_forge")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public HttpUrl provideToolsForgeUrl() {
        return HttpUrl.parse(TOOLS_FORGE_URL);
    }

    @Provides
    @Singleton
    @Named(NAMED_COMMONS_WIKI_SITE)
    public WikiSite provideCommonsWikiSite() {
        return new WikiSite(BuildConfig.COMMONS_URL);
    }

    @Provides
    @Singleton
    @Named(NAMED_WIKI_DATA_WIKI_SITE)
    public WikiSite provideWikidataWikiSite() {
        return new WikiSite(BuildConfig.WIKIDATA_URL);
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

    @Provides
    @Singleton
    @Named("commons-service")
    public Service provideCommonsService(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite);
    }

    @Provides
    @Singleton
    @Named("wikidata-service")
    public Service provideWikidataService(@Named(NAMED_WIKI_DATA_WIKI_SITE) WikiSite wikidataWikiSite) {
        return ServiceFactory.get(wikidataWikiSite, BuildConfig.WIKIDATA_URL, Service.class);
    }

    @Provides
    @Singleton
    public ReviewInterface provideReviewInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, ReviewInterface.class);
    }

    @Provides
    @Singleton
    public UploadInterface provideUploadInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, UploadInterface.class);
    }

    @Named("commons-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface providePageEditService(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, PageEditInterface.class);
    }

    @Named("wikidata-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface provideWikiDataPageEditService(@Named(NAMED_WIKI_DATA_WIKI_SITE) WikiSite wikiDataWikiSite) {
        return ServiceFactory.get(wikiDataWikiSite, BuildConfig.WIKIDATA_URL, PageEditInterface.class);
    }

    @Named("commons-page-edit")
    @Provides
    @Singleton
    public PageEditClient provideCommonsPageEditClient(@Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient,
                                                       @Named("commons-page-edit-service") PageEditInterface pageEditInterface,
                                                       @Named("commons-service") Service service) {
        return new PageEditClient(csrfTokenClient, pageEditInterface, service);
    }

    @Provides
    @Singleton
    public MediaInterface provideMediaInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, MediaInterface.class);
    }

    @Provides
    @Singleton
    public CategoryInterface provideCategoryInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, CategoryInterface.class);
    }

    @Provides
    @Singleton
    public UserInterface provideUserInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, UserInterface.class);
    }

    @Provides
    @Singleton
    public WikidataInterface provideWikidataInterface(@Named(NAMED_WIKI_DATA_WIKI_SITE) WikiSite wikiDataWikiSite) {
        return ServiceFactory.get(wikiDataWikiSite, BuildConfig.WIKIDATA_URL, WikidataInterface.class);
    }
}
