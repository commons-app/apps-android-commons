package fr.free.nrw.commons.di

import android.app.Activity
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.collection.LruCache
import androidx.room.Room.databaseBuilder
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.ui.selector.ImageFileLoader
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.PlaceDao
import fr.free.nrw.commons.review.ReviewDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.UploadController
import fr.free.nrw.commons.upload.depicts.DepictsDao
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.TimeProvider
import fr.free.nrw.commons.wikidata.WikidataEditListener
import fr.free.nrw.commons.wikidata.WikidataEditListenerImpl
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Objects
import javax.inject.Named
import javax.inject.Singleton

/**
 * The Dependency Provider class for Commons Android.
 * Provides all sorts of ContentProviderClients used by the app
 * along with the Liscences, AccountUtility, UploadController, Logged User,
 * Location manager etc
 */
@Module
@Suppress("unused")
open class CommonsApplicationModule(private val applicationContext: Context) {
    @Provides
    fun providesImageFileLoader(context: Context): ImageFileLoader =
        ImageFileLoader(context)

    @Provides
    fun providesApplicationContext(): Context =
        applicationContext

    @Provides
    fun provideInputMethodManager(): InputMethodManager =
        applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    @Named("licenses")
    fun provideLicenses(context: Context): List<String> = listOf(
        context.getString(R.string.license_name_cc0),
        context.getString(R.string.license_name_cc_by),
        context.getString(R.string.license_name_cc_by_sa),
        context.getString(R.string.license_name_cc_by_four),
        context.getString(R.string.license_name_cc_by_sa_four)
    )

    @Provides
    @Named("licenses_by_name")
    fun provideLicensesByName(context: Context): Map<String, String> = mapOf(
        context.getString(R.string.license_name_cc0) to Prefs.Licenses.CC0,
        context.getString(R.string.license_name_cc_by) to Prefs.Licenses.CC_BY_3,
        context.getString(R.string.license_name_cc_by_sa) to Prefs.Licenses.CC_BY_SA_3,
        context.getString(R.string.license_name_cc_by_four) to Prefs.Licenses.CC_BY_4,
        context.getString(R.string.license_name_cc_by_sa_four) to Prefs.Licenses.CC_BY_SA_4
    )

    /**
     * Provides an instance of CategoryContentProviderClient i.e. the categories
     * that are there in local storage
     */
    @Provides
    @Named("category")
    open fun provideCategoryContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.CATEGORY_AUTHORITY)

    @Provides
    @Named("recentsearch")
    fun provideRecentSearchContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.RECENT_SEARCH_AUTHORITY)

    @Provides
    @Named("contribution")
    open fun provideContributionContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.CONTRIBUTION_AUTHORITY)

    @Provides
    @Named("modification")
    open fun provideModificationContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.MODIFICATION_AUTHORITY)

    @Provides
    @Named("bookmarks")
    fun provideBookmarkContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.BOOKMARK_AUTHORITY)

    @Provides
    @Named("bookmarksLocation")
    fun provideBookmarkLocationContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.BOOKMARK_LOCATIONS_AUTHORITY)

    @Provides
    @Named("bookmarksItem")
    fun provideBookmarkItemContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.BOOKMARK_ITEMS_AUTHORITY)

    /**
     * This method is used to provide instance of RecentLanguagesContentProvider
     * which provides content of recent used languages from database
     * @param context Context
     * @return returns RecentLanguagesContentProvider
     */
    @Provides
    @Named("recent_languages")
    fun provideRecentLanguagesContentProviderClient(context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.RECENT_LANGUAGE_AUTHORITY)

    /**
     * Provides a Json store instance(JsonKvStore) which keeps
     * the provided Gson in it's instance
     * @param gson stored inside the store instance
     */
    @Provides
    @Named("default_preferences")
    open fun providesDefaultKvStore(context: Context, gson: Gson): JsonKvStore =
        JsonKvStore(context, "${context.packageName}_preferences", gson)

    @Provides
    fun providesUploadController(
        sessionManager: SessionManager,
        @Named("default_preferences") kvStore: JsonKvStore,
        context: Context
    ): UploadController = UploadController(sessionManager, context, kvStore)

    @Provides
    @Singleton
    open fun provideLocationServiceManager(context: Context): LocationServiceManager =
        LocationServiceManager(context)

    @Provides
    @Singleton
    open fun provideDBOpenHelper(context: Context): DBOpenHelper =
        DBOpenHelper(context)

    @Provides
    @Singleton
    @Named("thumbnail-cache")
    open fun provideLruCache(): LruCache<String, String> =
        LruCache(1024)

    @Provides
    @Singleton
    fun provideWikidataEditListener(): WikidataEditListener =
        WikidataEditListenerImpl()

    @Named("isBeta")
    @Provides
    @Singleton
    fun provideIsBetaVariant(): Boolean =
        isBetaFlavour

    @Named(IO_THREAD)
    @Provides
    fun providesIoThread(): Scheduler =
        Schedulers.io()

    @Named(MAIN_THREAD)
    @Provides
    fun providesMainThread(): Scheduler =
        AndroidSchedulers.mainThread()

    @Named("username")
    @Provides
    fun provideLoggedInUsername(sessionManager: SessionManager): String =
        Objects.toString(sessionManager.userName, "")

    @Provides
    @Singleton
    fun provideAppDataBase(): AppDatabase = databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "commons_room.db"
    ).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration().build()

    @Provides
    fun providesContributionsDao(appDatabase: AppDatabase): ContributionDao =
        appDatabase.contributionDao()

    @Provides
    fun providesPlaceDao(appDatabase: AppDatabase): PlaceDao =
        appDatabase.PlaceDao()

    @Provides
    fun providesDepictDao(appDatabase: AppDatabase): DepictsDao =
        appDatabase.DepictsDao()

    @Provides
    fun providesUploadedStatusDao(appDatabase: AppDatabase): UploadedStatusDao =
        appDatabase.UploadedStatusDao()

    @Provides
    fun providesNotForUploadStatusDao(appDatabase: AppDatabase): NotForUploadStatusDao =
        appDatabase.NotForUploadStatusDao()

    @Provides
    fun providesReviewDao(appDatabase: AppDatabase): ReviewDao =
        appDatabase.ReviewDao()

    @Provides
    fun providesContentResolver(context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun provideTimeProvider(): TimeProvider {
        return TimeProvider(System::currentTimeMillis)
    }

    companion object {
        const val IO_THREAD: String = "io_thread"
        const val MAIN_THREAD: String = "main_thread"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contribution " + " ADD COLUMN hasInvalidLocation INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
