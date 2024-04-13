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
import fr.free.nrw.commons.wikidata.CoroutinesCommonsServiceFactory;
import fr.free.nrw.commons.wikidata.GsonUtil;
import fr.free.nrw.commons.wikidata.RxCommonsServiceFactory;
import fr.free.nrw.commons.wikidata.WikidataConstants;
import fr.free.nrw.commons.wikidata.WikidataInterface;
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar;
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage;
import fr.free.nrw.commons.wikidata.model.WikiSite;
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
import timber.log.Timber;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class NetworkingModule {

    private static final String WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql";
    private static final String TOOLS_FORGE_URL = "https://tools.wmflabs.org/commons-android-app/tool-commons-android-app";
    private static final String NAMED_WIKI_DATA_WIKI_SITE = "wikidata-wikisite";
    private static final String NAMED_WIKI_PEDIA_WIKI_SITE = "wikipedia-wikisite";

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(@NonNull final Context context,
        @NonNull final HttpLoggingInterceptor httpLoggingInterceptor) {
        final File dir = new File(context.getCacheDir(), "okHttpCache");
        return new OkHttpClient.Builder().connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS).addInterceptor(httpLoggingInterceptor)
            .readTimeout(120, TimeUnit.SECONDS).cache(new Cache(dir, 10 * 1024 * 1024)).build();
    }

    @Provides
    @Singleton
    public RxCommonsServiceFactory serviceFactory(@NonNull final CommonsCookieJar cookieJar) {
        return new RxCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar));
    }

    @Provides
    @Singleton
    public CoroutinesCommonsServiceFactory coroutinesServiceFactory(
        @NonNull final CommonsCookieJar cookieJar) {
        return new CoroutinesCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar));
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideHttpLoggingInterceptor() {
        final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor(
            message -> Timber.tag("OkHttp").v(message));
        httpLoggingInterceptor.setLevel(BuildConfig.DEBUG ? Level.BODY : Level.BASIC);
        return httpLoggingInterceptor;
    }

    @Provides
    @Singleton
    public OkHttpJsonApiClient provideOkHttpJsonApiClient(@NonNull final OkHttpClient okHttpClient,
        @NonNull final DepictsClient depictsClient,
        @NonNull @Named("tools_forge") final HttpUrl toolsForgeUrl,
        @NonNull @Named("default_preferences") final JsonKvStore defaultKvStore,
        @NonNull final Gson gson) {
        return new OkHttpJsonApiClient(okHttpClient, depictsClient, toolsForgeUrl,
            WIKIDATA_SPARQL_QUERY_URL, BuildConfig.WIKIMEDIA_CAMPAIGNS_URL, gson);
    }

    @Provides
    @Singleton
    public CommonsCookieStorage provideCookieStorage(
        @NonNull @Named("default_preferences") final JsonKvStore preferences) {
        final CommonsCookieStorage cookieStorage = new CommonsCookieStorage(preferences);
        cookieStorage.load();
        return cookieStorage;
    }

    @Provides
    @Singleton
    public CommonsCookieJar provideCookieJar(@NonNull final CommonsCookieStorage storage) {
        return new CommonsCookieJar(storage);
    }

    @Named(WikidataConstants.NAMED_COMMONS_CSRF)
    @Provides
    @Singleton
    public CsrfTokenClient provideCommonsCsrfTokenClient(
        @NonNull final SessionManager sessionManager,
        @NonNull final CsrfTokenInterface tokenInterface, @NonNull final LoginClient loginClient,
        @NonNull final LogoutClient logoutClient) {
        return new CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient);
    }

    @Provides
    @Singleton
    public CsrfTokenInterface provideCsrfTokenInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, CsrfTokenInterface.class);
    }

    @Provides
    @Singleton
    public LoginInterface provideLoginInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, LoginInterface.class);
    }

    @Provides
    @Singleton
    public LoginClient provideLoginClient(@NonNull final LoginInterface loginInterface) {
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

    @Provides
    @Singleton
    public Gson provideGson() {
        return GsonUtil.getDefaultGson();
    }

    @Provides
    @Singleton
    public ReviewInterface provideReviewInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, ReviewInterface.class);
    }

    @Provides
    @Singleton
    public DepictsInterface provideDepictsInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, DepictsInterface.class);
    }

    @Provides
    @Singleton
    public WikiBaseInterface provideWikiBaseInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, WikiBaseInterface.class);
    }

    @Provides
    @Singleton
    public UploadInterface provideUploadInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, UploadInterface.class);
    }

    @Named("commons-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface providePageEditService(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, PageEditInterface.class);
    }

    @Named("wikidata-page-edit-service")
    @Provides
    @Singleton
    public PageEditInterface provideWikiDataPageEditService(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, PageEditInterface.class);
    }

    @Named("commons-page-edit")
    @Provides
    @Singleton
    public PageEditClient provideCommonsPageEditClient(
        @NonNull @Named(WikidataConstants.NAMED_COMMONS_CSRF) final CsrfTokenClient csrfTokenClient,
        @NonNull @Named("commons-page-edit-service") final PageEditInterface pageEditInterface) {
        return new PageEditClient(csrfTokenClient, pageEditInterface);
    }

    @Provides
    @Singleton
    public MediaInterface provideMediaInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, MediaInterface.class);
    }

    @Provides
    @Singleton
    public WikidataMediaInterface provideWikidataMediaInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BetaConstants.COMMONS_URL, WikidataMediaInterface.class);
    }

    @Provides
    @Singleton
    public MediaDetailInterface providesMediaDetailInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, MediaDetailInterface.class);
    }

    @Provides
    @Singleton
    public CategoryInterface provideCategoryInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, CategoryInterface.class);
    }

    @Provides
    @Singleton
    public ThanksInterface provideThanksInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, ThanksInterface.class);
    }

    @Provides
    @Singleton
    public NotificationInterface provideNotificationInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, NotificationInterface.class);
    }

    @Provides
    @Singleton
    public UserInterface provideUserInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.COMMONS_URL, UserInterface.class);
    }

    @Provides
    @Singleton
    public WikidataInterface provideWikidataInterface(
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(BuildConfig.WIKIDATA_URL, WikidataInterface.class);
    }

    /**
     * Add provider for PageMediaInterface It creates a retrofit service for the wiki site using
     * device's current language
     */
    @Provides
    @Singleton
    public PageMediaInterface providePageMediaInterface(
        @NonNull @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) final WikiSite wikiSite,
        @NonNull final RxCommonsServiceFactory serviceFactory) {
        return serviceFactory.create(wikiSite.url(), PageMediaInterface.class);
    }

    @Provides
    @Singleton
    @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    public WikiSite provideLanguageWikipediaSite() {
        return WikiSite.forLanguageCode(Locale.getDefault().getLanguage());
    }
}
