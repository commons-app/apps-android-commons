package fr.free.nrw.commons.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.actions.PageEditInterface
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.auth.csrf.CsrfTokenInterface
import fr.free.nrw.commons.auth.csrf.LogoutClient
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginInterface
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieStorage
import fr.free.nrw.commons.wikidata.model.WikiSite
import kotlinx.coroutines.Dispatchers
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

    @Provides
    @Singleton
    @Named(WikidataConstants.NAMED_COMMONS_CSRF)
    fun provideCommonsCsrfTokenClient(
        sessionManager: SessionManager,
        tokenInterface: CsrfTokenInterface,
        loginClient: LoginClient,
        logoutClient: LogoutClient
    ): CsrfTokenClient = CsrfTokenClient(
        sessionManager, tokenInterface, loginClient, logoutClient, Dispatchers.IO
    )

    @Provides
    @Singleton
    fun provideLoginClient(loginInterface: LoginInterface): LoginClient =
        LoginClient(loginInterface)

    @Provides
    @Singleton
    @Named("commons-page-edit")
    fun provideCommonsPageEditClient(
        @Named(WikidataConstants.NAMED_COMMONS_CSRF) csrfTokenClient: CsrfTokenClient,
        @Named("commons-page-edit-service") pageEditInterface: PageEditInterface
    ): PageEditClient = PageEditClient(csrfTokenClient, pageEditInterface)

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
    @Named(WikidataConstants.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    fun provideLanguageWikipediaSite(): WikiSite =
        WikiSite.forLanguageCode(Locale.getDefault().language)
}
