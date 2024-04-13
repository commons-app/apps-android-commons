package fr.free.nrw.commons.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.BetaConstants
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.OkHttpConnectionFactory
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
import fr.free.nrw.commons.wikidata.CoroutinesCommonsServiceFactory
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.RxCommonsServiceFactory
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.WikidataInterface
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage
import fr.free.nrw.commons.wikidata.model.WikiSite
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import timber.log.Timber
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private const val WIKIDATA_SPARQL_QUERY_URL = "https://query.wikidata.org/sparql"
private const val TOOLS_FORGE_URL = "https://tools.wmflabs.org/commons-android-app/tool-commons-android-app"
private const val NAMED_WIKI_DATA_WIKI_SITE = "wikidata-wikisite"

@Module
@Suppress("unused")
class NetworkingModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        context: Context, httpLoggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        val dir = File(context.cacheDir, "okHttpCache")
        return OkHttpClient.Builder().connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS).addInterceptor(httpLoggingInterceptor)
            .readTimeout(120, TimeUnit.SECONDS).cache(Cache(dir, (10 * 1024 * 1024).toLong()))
            .build()
    }

    @Provides
    @Singleton
    fun serviceFactory(cookieJar: CommonsCookieJar): RxCommonsServiceFactory =
        RxCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar))

    @Provides
    @Singleton
    fun coroutinesServiceFactory(cookieJar: CommonsCookieJar): CoroutinesCommonsServiceFactory =
        CoroutinesCommonsServiceFactory(OkHttpConnectionFactory.getClient(cookieJar))

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor {
        Timber.tag("OkHttp").v(it)
    }.apply {
        level = if (BuildConfig.DEBUG) BODY else BASIC
    }

    @Provides
    @Singleton
    fun provideOkHttpJsonApiClient(
        okHttpClient: OkHttpClient,
        depictsClient: DepictsClient,
        @Named("tools_forge") toolsForgeUrl: HttpUrl,
        gson: Gson
    ): OkHttpJsonApiClient = OkHttpJsonApiClient(
        okHttpClient,
        depictsClient,
        toolsForgeUrl,
        WIKIDATA_SPARQL_QUERY_URL,
        BuildConfig.WIKIMEDIA_CAMPAIGNS_URL,
        gson
    )

    @Provides
    @Singleton
    fun provideCookieStorage(
        @Named("default_preferences") preferences: JsonKvStore
    ): CommonsCookieStorage = CommonsCookieStorage(preferences).also { it.load() }

    @Provides
    @Singleton
    fun provideCookieJar(storage: CommonsCookieStorage): CommonsCookieJar =
        CommonsCookieJar(storage)

    @Named(WikidataConstants.NAMED_COMMONS_CSRF)
    @Provides
    @Singleton
    fun provideCommonsCsrfTokenClient(
        sessionManager: SessionManager,
        tokenInterface: CsrfTokenInterface,
        loginClient: LoginClient,
        logoutClient: LogoutClient
    ): CsrfTokenClient = CsrfTokenClient(sessionManager, tokenInterface, loginClient, logoutClient)

    @Provides
    @Singleton
    fun provideCsrfTokenInterface(serviceFactory: RxCommonsServiceFactory): CsrfTokenInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, CsrfTokenInterface::class.java)

    @Provides
    @Singleton
    fun provideLoginInterface(serviceFactory: RxCommonsServiceFactory): LoginInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, LoginInterface::class.java)

    @Provides
    @Singleton
    fun provideLoginClient(loginInterface: LoginInterface): LoginClient =
        LoginClient(loginInterface)

    @Provides
    @Named("wikimedia_api_host")
    fun provideMwApiUrl(): String = BuildConfig.WIKIMEDIA_API_HOST

    @Provides
    @Named("tools_forge")
    fun provideToolsForgeUrl(): HttpUrl = TOOLS_FORGE_URL.toHttpUrlOrNull()!!

    @Provides
    @Singleton
    @Named(NAMED_WIKI_DATA_WIKI_SITE)
    fun provideWikidataWikiSite(): WikiSite = WikiSite(BuildConfig.WIKIDATA_URL)

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonUtil.getDefaultGson()

    @Provides
    @Singleton
    fun provideReviewInterface(serviceFactory: RxCommonsServiceFactory): ReviewInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, ReviewInterface::class.java)

    @Provides
    @Singleton
    fun provideDepictsInterface(serviceFactory: RxCommonsServiceFactory): DepictsInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, DepictsInterface::class.java)

    @Provides
    @Singleton
    fun provideWikiBaseInterface(serviceFactory: RxCommonsServiceFactory): WikiBaseInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, WikiBaseInterface::class.java)

    @Provides
    @Singleton
    fun provideUploadInterface(serviceFactory: RxCommonsServiceFactory): UploadInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, UploadInterface::class.java)

    @Provides
    @Singleton
    @Named("commons-page-edit-service")
    fun providePageEditService(serviceFactory: RxCommonsServiceFactory): PageEditInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, PageEditInterface::class.java)

    @Provides
    @Singleton
    @Named("wikidata-page-edit-service")
    fun provideWikiDataPageEditService(serviceFactory: RxCommonsServiceFactory): PageEditInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, PageEditInterface::class.java)

    @Provides
    @Singleton
    @Named("commons-page-edit")
    fun provideCommonsPageEditClient(
        @Named(WikidataConstants.NAMED_COMMONS_CSRF) csrfTokenClient: CsrfTokenClient,
        @Named("commons-page-edit-service") pageEditInterface: PageEditInterface
    ): PageEditClient = PageEditClient(csrfTokenClient, pageEditInterface)

    @Provides
    @Singleton
    fun provideMediaInterface(serviceFactory: RxCommonsServiceFactory): MediaInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, MediaInterface::class.java)

    @Provides
    @Singleton
    fun provideWikidataMediaInterface(serviceFactory: RxCommonsServiceFactory): WikidataMediaInterface =
        serviceFactory.create(BetaConstants.COMMONS_URL, WikidataMediaInterface::class.java)

    @Provides
    @Singleton
    fun providesMediaDetailInterface(serviceFactory: RxCommonsServiceFactory): MediaDetailInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, MediaDetailInterface::class.java)

    @Provides
    @Singleton
    fun provideCategoryInterface(serviceFactory: RxCommonsServiceFactory): CategoryInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, CategoryInterface::class.java)

    @Provides
    @Singleton
    fun provideThanksInterface(serviceFactory: RxCommonsServiceFactory): ThanksInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, ThanksInterface::class.java)

    @Provides
    @Singleton
    fun provideNotificationInterface(serviceFactory: RxCommonsServiceFactory): NotificationInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, NotificationInterface::class.java)

    @Provides
    @Singleton
    fun provideUserInterface(serviceFactory: RxCommonsServiceFactory): UserInterface =
        serviceFactory.create(BuildConfig.COMMONS_URL, UserInterface::class.java)

    @Provides
    @Singleton
    fun provideWikidataInterface(serviceFactory: RxCommonsServiceFactory): WikidataInterface =
        serviceFactory.create(BuildConfig.WIKIDATA_URL, WikidataInterface::class.java)

    /**
     * Add provider for PageMediaInterface It creates a retrofit service for the wiki site using
     * device's current language
     */
    @Provides
    @Singleton
    fun providePageMediaInterface(
        @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE) wikiSite: WikiSite,
        serviceFactory: RxCommonsServiceFactory
    ): PageMediaInterface = serviceFactory.create(wikiSite.url(), PageMediaInterface::class.java)

    @Provides
    @Singleton
    @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    fun provideLanguageWikipediaSite(): WikiSite =
        WikiSite.forLanguageCode(Locale.getDefault().language)
}
