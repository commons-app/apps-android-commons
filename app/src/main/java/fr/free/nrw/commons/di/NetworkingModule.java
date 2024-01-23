package fr.free.nrw.commons.di;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BetaConstants;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.actions.PageEditInterface;
import fr.free.nrw.commons.actions.ThanksInterface;
import fr.free.nrw.commons.category.CategoryInterface;
import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaDetailInterface;
import fr.free.nrw.commons.media.MediaInterface;
import fr.free.nrw.commons.media.PageMediaInterface;
import fr.free.nrw.commons.media.WikidataMediaInterface;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.mwapi.UserInterface;
import fr.free.nrw.commons.notification.NotificationInterface;
import fr.free.nrw.commons.review.ReviewInterface;
import fr.free.nrw.commons.upload.UploadInterface;
import fr.free.nrw.commons.upload.WikiBaseInterface;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.wikidata.WikidataInterface;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.wikipedia.csrf.CsrfTokenClient;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.login.LoginClient;
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
    private static final String NAMED_WIKI_PEDIA_WIKI_SITE = "wikipedia-wikisite";

    public static final String NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE = "language-wikipedia-wikisite";

    public static final String NAMED_COMMONS_CSRF = "commons-csrf";

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(Context context,
                                            HttpLoggingInterceptor httpLoggingInterceptor) {
        File dir = new File(context.getCacheDir(), "okHttpCache");
        return new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor)
            .readTimeout(120, TimeUnit.SECONDS)
            .cache(new Cache(dir, OK_HTTP_CACHE_SIZE))
            .build();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(message -> {
            Timber.tag("OkHttp").v(message);
        });
        httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? Level.BODY: Level.BASIC);
        return httpLoggingInterceptor;
    }

    @Provides
    @Singleton
    public OkHttpJsonApiClient provideOkHttpJsonApiClient(OkHttpClient okHttpClient,
                                                          DepictsClient depictsClient,
                                                          @Named("tools_forge") HttpUrl toolsForgeUrl,
                                                          @Named("test_tools_forge") HttpUrl testToolsForgeUrl,
                                                          @Named("default_preferences") JsonKvStore defaultKvStore,
                                                          Gson gson) {
        return new OkHttpJsonApiClient(okHttpClient,
                depictsClient,
                toolsForgeUrl,
                testToolsForgeUrl,
                WIKIDATA_SPARQL_QUERY_URL,
                BuildConfig.WIKIMEDIA_CAMPAIGNS_URL,
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
    @Named("test_tools_forge")
    @NonNull
    @SuppressWarnings("ConstantConditions")
    public HttpUrl provideTestToolsForgeUrl() {
        return HttpUrl.parse(TEST_TOOLS_FORGE_URL);
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
    public DepictsInterface provideDepictsInterface(@Named(NAMED_WIKI_DATA_WIKI_SITE) WikiSite wikidataWikiSite) {
        return ServiceFactory.get(wikidataWikiSite, BuildConfig.WIKIDATA_URL, DepictsInterface.class);
    }

    @Provides
    @Singleton
    public WikiBaseInterface provideWikiBaseInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, WikiBaseInterface.class);
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
                                                       @Named("commons-page-edit-service") PageEditInterface pageEditInterface) {
        return new PageEditClient(csrfTokenClient, pageEditInterface);
    }

    @Provides
    @Singleton
    public MediaInterface provideMediaInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite, BuildConfig.COMMONS_URL, MediaInterface.class);
    }

    /**
     * Add provider for WikidataMediaInterface
     * It creates a retrofit service for the commons wiki site
     * @param commonsWikiSite commonsWikiSite
     * @return WikidataMediaInterface
     */
    @Provides
    @Singleton
    public WikidataMediaInterface provideWikidataMediaInterface(
        @Named(NAMED_COMMONS_WIKI_SITE) final WikiSite commonsWikiSite) {
        return ServiceFactory.get(commonsWikiSite,
            BetaConstants.COMMONS_URL, WikidataMediaInterface.class);
    }

    @Provides
    @Singleton
    public MediaDetailInterface providesMediaDetailInterface(@Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikisite) {
        return ServiceFactory.get(commonsWikisite, BuildConfig.COMMONS_URL, MediaDetailInterface.class);
    }

    @Provides
    @Singleton
    public CategoryInterface provideCategoryInterface(
        @Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory
               .get(commonsWikiSite, BuildConfig.COMMONS_URL, CategoryInterface.class);
    }

    @Provides
    @Singleton
    public ThanksInterface provideThanksInterface(
        @Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory
               .get(commonsWikiSite, BuildConfig.COMMONS_URL, ThanksInterface.class);
    }

    @Provides
    @Singleton
    public NotificationInterface provideNotificationInterface(
        @Named(NAMED_COMMONS_WIKI_SITE) WikiSite commonsWikiSite) {
        return ServiceFactory
               .get(commonsWikiSite, BuildConfig.COMMONS_URL, NotificationInterface.class);
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

    /**
     * Add provider for PageMediaInterface
     * It creates a retrofit service for the wiki site using device's current language
     */
    @Provides
    @Singleton
    public PageMediaInterface providePageMediaInterface(@Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) WikiSite wikiSite) {
        return ServiceFactory.get(wikiSite, wikiSite.url(), PageMediaInterface.class);
    }

    @Provides
    @Singleton
    @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    public WikiSite provideLanguageWikipediaSite() {
        return WikiSite.forLanguageCode(Locale.getDefault().getLanguage());
    }
}
