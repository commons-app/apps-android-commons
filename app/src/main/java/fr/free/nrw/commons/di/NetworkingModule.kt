package fr.free.nrw.commons.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.free.nrw.commons.BetaConstants
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.CommonHeaderRequestInterceptor
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.actions.PageEditInterface
import fr.free.nrw.commons.actions.ThanksInterface
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.CsrfTokenInterface
import fr.free.nrw.commons.auth.csrf.LogoutClient
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginInterface
import fr.free.nrw.commons.category.CategoryInterface
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.media.MediaDetailInterface
import fr.free.nrw.commons.media.MediaInterface
import fr.free.nrw.commons.media.PageMediaInterface
import fr.free.nrw.commons.media.WikidataMediaInterface
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.mwapi.UserInterface
import fr.free.nrw.commons.notification.NotificationInterface
import fr.free.nrw.commons.review.ReviewInterface
import fr.free.nrw.commons.upload.UploadInterface
import fr.free.nrw.commons.upload.WikiBaseInterface
import fr.free.nrw.commons.upload.depicts.DepictsInterface
import fr.free.nrw.commons.wikidata.CommonsServiceFactory
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.WikidataInterface
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage
import fr.free.nrw.commons.wikidata.model.WikiSite
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
class NetworkingModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        context: Context,
        httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(CommonHeaderRequestInterceptor())
        .readTimeout(120, TimeUnit.SECONDS)
        .cache(Cache(File(context.cacheDir, "okHttpCache"), OK_HTTP_CACHE_SIZE))
        .build()

    @Provides
    @Singleton
    fun serviceFactory(cookieJar: CommonsCookieJar): CommonsServiceFactory =
        CommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar))

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message: String? ->
            Timber.tag("OkHttp").v(message)
        }.apply {
            level = if (BuildConfig.DEBUG) Level.BODY else Level.BASIC
        }

    @Provides
    @Singleton
    fun provideOkHttpJsonApiClient(
        okHttpClient: OkHttpClient,
        depictsClient: DepictsClient,
        @Named("tools_forge") toolsForgeUrl: HttpUrl,
        gson: Gson
    ): OkHttpJsonApiClient = OkHttpJsonApiClient(
        okHttpClient, depictsClient, toolsForgeUrl, WIKIDATA_SPARQL_QUERY_URL,
        BuildConfig.WIKIMEDIA_CAMPAIGNS_URL, gson
    )

    @Provides
    @Singleton
    fun provideCookieStorage(
        @Named("default_preferences") preferences: JsonKvStore
    ): CommonsCookieStorage = CommonsCookieStorage(preferences).also {
        it.load()
    }

    @Provides
    @Singleton
    fun provideCookieJar(storage: CommonsCookieStorage): CommonsCookieJar =
        CommonsCookieJar(storage)

    @Named(NAMED_COMMONS_CSRF)
    @Provides
    @Singleton
    fun provideCommonsCsrfTokenClient(
        sessionManager: SessionManager,
        @Named("commons-csrf-interface") tokenInterface: CsrfTokenInterface,
        loginClient: LoginClient,
        logoutClient: LogoutClient
    ): CsrfTokenClient = CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient)

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
    fun provideWikiCsrfTokenClient(
        sessionManager: SessionManager,
        @Named("wikidata-csrf-interface") tokenInterface: CsrfTokenInterface,
        loginClient: LoginClient,
        logoutClient: LogoutClient
    ): CsrfTokenClient = CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient)

    /**
     * Provides a singleton instance of CsrfTokenInterface for Wikidata.
     *
     * @param factory The factory used to create service interfaces.
     * @return A singleton instance of CsrfTokenInterface for Wikidata.
     */
    @Named("wikidata-csrf-interface")
    @Provides
    @Singleton
    fun provideWikidataCsrfTokenInterface(factory: CommonsServiceFactory): CsrfTokenInterface =
        factory.create(BuildConfig.WIKIDATA_URL)

    @Named("commons-csrf-interface")
    @Provides
    @Singleton
    fun provideCsrfTokenInterface(factory: CommonsServiceFactory): CsrfTokenInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideLoginInterface(factory: CommonsServiceFactory): LoginInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideLoginClient(loginInterface: LoginInterface): LoginClient =
        LoginClient(loginInterface)

    @Provides
    @Named("tools_forge")
    fun provideToolsForgeUrl(): HttpUrl = TOOLS_FORGE_URL.toHttpUrlOrNull()!!

    @Provides
    @Singleton
    @Named(NAMED_WIKI_DATA_WIKI_SITE)
    fun provideWikidataWikiSite(): WikiSite = WikiSite(BuildConfig.WIKIDATA_URL)

    /**
     * Gson objects are very heavy. The app should ideally be using just one instance of it instead of creating new instances everywhere.
     * @return returns a singleton Gson instance
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonUtil.defaultGson

    @Provides
    @Singleton
    fun provideReviewInterface(factory: CommonsServiceFactory): ReviewInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideDepictsInterface(factory: CommonsServiceFactory): DepictsInterface =
        factory.create(BuildConfig.WIKIDATA_URL)

    @Provides
    @Singleton
    fun provideWikiBaseInterface(factory: CommonsServiceFactory): WikiBaseInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideUploadInterface(factory: CommonsServiceFactory): UploadInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Named("commons-page-edit-service")
    @Provides
    @Singleton
    fun providePageEditService(factory: CommonsServiceFactory): PageEditInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Named("wikidata-page-edit-service")
    @Provides
    @Singleton
    fun provideWikiDataPageEditService(factory: CommonsServiceFactory): PageEditInterface =
        factory.create(BuildConfig.WIKIDATA_URL)

    @Named("commons-page-edit")
    @Provides
    @Singleton
    fun provideCommonsPageEditClient(
        @Named(NAMED_COMMONS_CSRF) csrfTokenClient: CsrfTokenClient,
        @Named("commons-page-edit-service") pageEditInterface: PageEditInterface
    ): PageEditClient = PageEditClient(csrfTokenClient, pageEditInterface)

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
    fun provideWikidataPageEditClient(
        @Named(NAMED_WIKI_CSRF) csrfTokenClient: CsrfTokenClient,
        @Named("wikidata-page-edit-service") pageEditInterface: PageEditInterface
    ): PageEditClient = PageEditClient(csrfTokenClient, pageEditInterface)

    @Provides
    @Singleton
    fun provideMediaInterface(factory: CommonsServiceFactory): MediaInterface =
        factory.create(BuildConfig.COMMONS_URL)

    /**
     * Provides a Retrofit service for accessing the commons wiki site via [WikidataMediaInterface].
     *
     * @param factory The CommonsServiceFactory used to create the Retrofit service.
     * @return An instance of [WikidataMediaInterface].
     */
    @Provides
    @Singleton
    fun provideWikidataMediaInterface(factory: CommonsServiceFactory): WikidataMediaInterface =
        factory.create(BetaConstants.COMMONS_URL)

    @Provides
    @Singleton
    fun providesMediaDetailInterface(factory: CommonsServiceFactory): MediaDetailInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideCategoryInterface(factory: CommonsServiceFactory): CategoryInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideThanksInterface(factory: CommonsServiceFactory): ThanksInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideNotificationInterface(factory: CommonsServiceFactory): NotificationInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideUserInterface(factory: CommonsServiceFactory): UserInterface =
        factory.create(BuildConfig.COMMONS_URL)

    @Provides
    @Singleton
    fun provideWikidataInterface(factory: CommonsServiceFactory): WikidataInterface =
        factory.create(BuildConfig.WIKIDATA_URL)

    /**
     * Add provider for PageMediaInterface
     * It creates a retrofit service for the wiki site using device's current language
     */
    @Provides
    @Singleton
    fun providePageMediaInterface(
        @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) wikiSite: WikiSite,
        factory: CommonsServiceFactory
    ): PageMediaInterface = factory.create(wikiSite.url())

    @Provides
    @Singleton
    @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    fun provideLanguageWikipediaSite(): WikiSite =
        WikiSite.forDefaultLocaleLanguageCode()

    companion object {
        private const val WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql"
        private const val TOOLS_FORGE_URL =
            "https://tools.wmflabs.org/commons-android-app/tool-commons-android-app"

        const val OK_HTTP_CACHE_SIZE: Long = (10 * 1024 * 1024).toLong()

        private const val NAMED_WIKI_DATA_WIKI_SITE = "wikidata-wikisite"
        private const val NAMED_WIKI_PEDIA_WIKI_SITE = "wikipedia-wikisite"

        const val NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE: String = "language-wikipedia-wikisite"

        const val NAMED_COMMONS_CSRF: String = "commons-csrf"
        const val NAMED_WIKI_CSRF: String = "wiki-csrf"
    }
}
