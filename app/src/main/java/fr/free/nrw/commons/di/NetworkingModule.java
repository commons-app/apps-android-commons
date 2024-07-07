package fr.free.nrw.commons.di;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BetaConstants;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.OkHttpConnectionFactory;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.actions.PageEditInterface;
import fr.free.nrw.commons.actions.ThanksInterface;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient;
import fr.free.nrw.commons.auth.csrf.CsrfTokenInterface;
import fr.free.nrw.commons.auth.csrf.LogoutClient;
import fr.free.nrw.commons.auth.login.LoginClient;
import fr.free.nrw.commons.auth.login.LoginInterface;
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
import fr.free.nrw.commons.wikidata.CommonsServiceFactory;
import fr.free.nrw.commons.wikidata.WikidataInterface;
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar;
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage;
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
import fr.free.nrw.commons.wikidata.model.WikiSite;
import fr.free.nrw.commons.wikidata.GsonUtil;
import timber.log.Timber;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class NetworkingModule {
    private static final String WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql";
    private static final String TOOLS_FORGE_URL = "https://tools.wmflabs.org/commons-android-app/tool-commons-android-app";

    public static final long OK_HTTP_CACHE_SIZE = 10 * 1024 * 1024;

    private static final String NAMED_WIKI_DATA_WIKI_SITE = "wikidata-wikisite";
    private static final String NAMED_WIKI_PEDIA_WIKI_SITE = "wikipedia-wikisite";

    public static final String NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE = "language-wikipedia-wikisite";

    public static final String NAMED_COMMONS_CSRF = "commons-csrf";
    public static final String NAMED_WIKI_CSRF = "wiki-csrf";

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
    public CommonsServiceFactory serviceFactory(CommonsCookieJar cookieJar) {
        return new CommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar));
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
                                                          @Named("default_preferences") JsonKvStore defaultKvStore,
                                                          Gson gson) {
        return new OkHttpJsonApiClient(okHttpClient,
                depictsClient,
                toolsForgeUrl,
                WIKIDATA_SPARQL_QUERY_URL,
                BuildConfig.WIKIMEDIA_CAMPAIGNS_URL,
            gson);
    }

    @Provides
    @Singleton
    public CommonsCookieStorage provideCookieStorage(
        @Named("default_preferences") JsonKvStore preferences) {
        CommonsCookieStorage cookieStorage = new CommonsCookieStorage(preferences);
        cookieStorage.load();
        return cookieStorage;
    }

    @Provides
    @Singleton
    public CommonsCookieJar provideCookieJar(CommonsCookieStorage storage) {
        return new CommonsCookieJar(storage);
    }

    @Named(NAMED_COMMONS_CSRF)
    @Provides
    @Singleton
    public CsrfTokenClient provideCommonsCsrfTokenClient(SessionManager sessionManager,
        @Named("commons-csrf-interface") CsrfTokenInterface tokenInterface, LoginClient loginClient, LogoutClient logoutClient) {
        return new CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient);
    }

    /**
     * Provides a singleton instance of CsrfTokenClient for Wikidata.
     *
     * @param sessionManager The session manager to manage user sessions.
     * @param tokenInterface The interface for obtaining CSRF tokens.
     * @param loginClient    The client for handling login operations.
     * @param logoutClient   The client for handling logout operations.
     * @return A singleton instance of CsrfTokenClient.
     */
    @Named(NAMED_WIKI_CSRF)
    @Provides
    @Singleton
    public CsrfTokenClient provideWikiCsrfTokenClient(SessionManager sessionManager,
        @Named("wikidata-csrf-interface") CsrfTokenInterface tokenInterface, LoginClient loginClient, LogoutClient logoutClient) {
        return new CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient);
    }

    /**
     * Provides a singleton instance of CsrfTokenInterface for Wikidata.
     *
     * @param serviceFactory The factory used to create service interfaces.
     * @return A singleton instance of CsrfTokenInterface for Wikidata.
     */
    @Named("wikidata-csrf-interface")
    @Provides
    @Singleton
    public CsrfTokenInterface provideWikidataCsrfTokenInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, CsrfTokenInterface.class);
    }

    @Named("commons-csrf-interface")
    @Provides
    @Singleton
    public CsrfTokenInterface provideCsrfTokenInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, CsrfTokenInterface.class);
    }

    @Provides
    @Singleton
    public LoginInterface provideLoginInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, LoginInterface.class);
    }

    @Provides
    @Singleton
    public LoginClient provideLoginClient(LoginInterface loginInterface) {
        return new LoginClient(loginInterface);
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
    public ReviewInterface provideReviewInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, ReviewInterface.class);
    }

    @Provides
    @Singleton
    public DepictsInterface provideDepictsInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, DepictsInterface.class);
    }

    @Provides
    @Singleton
    public WikiBaseInterface provideWikiBaseInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, WikiBaseInterface.class);
    }

    @Provides
    @Singleton
    public UploadInterface provideUploadInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, UploadInterface.class);
    }

    @Named("commons-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface providePageEditService(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, PageEditInterface.class);
    }

    @Named("wikidata-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface provideWikiDataPageEditService(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, PageEditInterface.class);
    }

    @Named("commons-page-edit")
    @Provides
    @Singleton
    public PageEditClient provideCommonsPageEditClient(@Named(NAMED_COMMONS_CSRF) CsrfTokenClient csrfTokenClient,
                                                       @Named("commons-page-edit-service") PageEditInterface pageEditInterface) {
        return new PageEditClient(csrfTokenClient, pageEditInterface);
    }

    /**
     * Provides a singleton instance of PageEditClient for Wikidata.
     *
     * @param csrfTokenClient    The client used to manage CSRF tokens.
     * @param pageEditInterface  The interface for page edit operations.
     * @return A singleton instance of PageEditClient for Wikidata.
     */
    @Named("wikidata-page-edit")
    @Provides
    @Singleton
    public PageEditClient provideWikidataPageEditClient(@Named(NAMED_WIKI_CSRF) CsrfTokenClient csrfTokenClient,
        @Named("wikidata-page-edit-service") PageEditInterface pageEditInterface) {
        return new PageEditClient(csrfTokenClient, pageEditInterface);
    }

    @Provides
    @Singleton
    public MediaInterface provideMediaInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, MediaInterface.class);
    }

    /**
     * Add provider for WikidataMediaInterface
     * It creates a retrofit service for the commons wiki site
     * @param commonsWikiSite commonsWikiSite
     * @return WikidataMediaInterface
     */
    @Provides
    @Singleton
    public WikidataMediaInterface provideWikidataMediaInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BetaConstants.COMMONS_URL, WikidataMediaInterface.class);
    }

    @Provides
    @Singleton
    public MediaDetailInterface providesMediaDetailInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, MediaDetailInterface.class);
    }

    @Provides
    @Singleton
    public CategoryInterface provideCategoryInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, CategoryInterface.class);
    }

    @Provides
    @Singleton
    public ThanksInterface provideThanksInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, ThanksInterface.class);
    }

    @Provides
    @Singleton
    public NotificationInterface provideNotificationInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, NotificationInterface.class);
    }

    @Provides
    @Singleton
    public UserInterface provideUserInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, UserInterface.class);
    }

    @Provides
    @Singleton
    public WikidataInterface provideWikidataInterface(CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, WikidataInterface.class);
    }

    /**
     * Add provider for PageMediaInterface
     * It creates a retrofit service for the wiki site using device's current language
     */
    @Provides
    @Singleton
    public PageMediaInterface providePageMediaInterface(@Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) WikiSite wikiSite, CommonsServiceFactory serviceFactory) {
        return serviceFactory.create(wikiSite.url(), PageMediaInterface.class);
    }

    @Provides
    @Singleton
    @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    public WikiSite provideLanguageWikipediaSite() {
        return WikiSite.forLanguageCode(Locale.getDefault().getLanguage());
    }
}
