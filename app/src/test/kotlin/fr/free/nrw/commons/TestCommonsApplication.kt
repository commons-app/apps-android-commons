package fr.free.nrw.commons

import android.app.Application
import android.content.ContentProviderClient
import android.content.Context
import androidx.collection.LruCache
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import dagger.hilt.android.testing.HiltAndroidTest

// TODO: Update test setup to use Hilt testing components
// See: https://developer.android.com/training/dependency-injection/hilt-testing
@HiltAndroidTest
class TestCommonsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.Theme_AppCompat)
        context = applicationContext
    }

    companion object {
        private var context: Context? = null

        fun getContext(): Context? = context
    }
}

// TODO: Replace with Hilt test modules
// For Hilt testing, use @TestInstallIn to replace production modules
@Suppress("MemberVisibilityCanBePrivate")
class MockCommonsApplicationModule {

    val defaultSharedPreferences: JsonKvStore = mock()
    val locationServiceManager: LocationServiceManager = mock()
    val mockDbOpenHelper: DBOpenHelper = mock()
    val lruCache: LruCache<String, String> = mock()
    val gson: Gson = Gson()
    val categoryClient: ContentProviderClient = mock()
    val contributionClient: ContentProviderClient = mock()
    val modificationClient: ContentProviderClient = mock()
    val uploadPrefs: JsonKvStore = mock()

    override fun provideCategoryContentProviderClient(context: Context): ContentProviderClient = categoryClient

    override fun provideContributionContentProviderClient(context: Context): ContentProviderClient = contributionClient

    override fun provideModificationContentProviderClient(context: Context): ContentProviderClient = modificationClient

    override fun providesDefaultKvStore(context: Context, gson: Gson): JsonKvStore = defaultSharedPreferences

    override fun provideLocationServiceManager(context: Context): LocationServiceManager = locationServiceManager

    override fun provideDBOpenHelper(context: Context): DBOpenHelper = mockDbOpenHelper

    override fun provideLruCache(): LruCache<String, String> = lruCache
}
