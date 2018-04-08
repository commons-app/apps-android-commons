package fr.free.nrw.commons

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.util.LruCache
import com.nhaarman.mockito_kotlin.mock
import com.squareup.leakcanary.RefWatcher
import fr.free.nrw.commons.auth.AccountUtil
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.caching.CacheController
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsApplicationComponent
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.di.DaggerCommonsApplicationComponent
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.nearby.NearbyPlaces
import fr.free.nrw.commons.upload.UploadController

class TestCommonsApplication : CommonsApplication() {
    private var mockApplicationComponent: CommonsApplicationComponent? = null

    override fun onCreate() {
        if (mockApplicationComponent == null) {
            mockApplicationComponent = DaggerCommonsApplicationComponent.builder()
                    .appModule(MockCommonsApplicationModule(this)).build()
        }
        super.onCreate()
    }

    // No leakcanary in unit tests.
    override fun setupLeakCanary(): RefWatcher = RefWatcher.DISABLED
}

class MockCommonsApplicationModule(appContext: Context) : CommonsApplicationModule(appContext) {
    val accountUtil: AccountUtil = mock()
    val appSharedPreferences: SharedPreferences = mock()
    val defaultSharedPreferences: SharedPreferences = mock()
    val otherSharedPreferences: SharedPreferences = mock()
    val uploadController: UploadController = mock()
    val mockSessionManager: SessionManager = mock()
    val mediaWikiApi: MediaWikiApi = mock()
    val locationServiceManager: LocationServiceManager = mock()
    val cacheController: CacheController = mock()
    val mockDbOpenHelper: DBOpenHelper = mock()
    val nearbyPlaces: NearbyPlaces = mock()
    val lruCache: LruCache<String, String> = mock()

    override fun providesAccountUtil(context: Context): AccountUtil = accountUtil

    override fun providesApplicationSharedPreferences(context: Context): SharedPreferences = appSharedPreferences

    override fun providesDefaultSharedPreferences(context: Context): SharedPreferences = defaultSharedPreferences

    override fun providesOtherSharedPreferences(context: Context): SharedPreferences = otherSharedPreferences

    override fun providesUploadController(sessionManager: SessionManager, sharedPreferences: SharedPreferences, context: Context): UploadController = uploadController

    override fun providesSessionManager(context: Context, mediaWikiApi: MediaWikiApi, sharedPreferences: SharedPreferences): SessionManager = mockSessionManager

    override fun provideMediaWikiApi(context: Context, sharedPreferences: SharedPreferences): MediaWikiApi = mediaWikiApi

    override fun provideLocationServiceManager(context: Context): LocationServiceManager = locationServiceManager

    override fun provideCacheController(): CacheController = cacheController

    override fun provideDBOpenHelper(context: Context): DBOpenHelper = mockDbOpenHelper

    override fun provideNearbyPlaces(): NearbyPlaces = nearbyPlaces

    override fun provideLruCache(): LruCache<String, String> = lruCache
}