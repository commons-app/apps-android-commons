package fr.free.nrw.commons.di

import android.app.Activity
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.inputmethod.InputMethodManager
import androidx.collection.LruCache
import androidx.room.Room.databaseBuilder
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
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
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object CommonsApplicationModule {

    @Provides
    fun providesImageFileLoader(@ApplicationContext context: Context): ImageFileLoader =
        ImageFileLoader(context)

    @Provides
    @Singleton
    fun providesApplicationContext(@ApplicationContext context: Context): Context {
        appContext = context
        return context
    }

    @Provides
    fun provideInputMethodManager(@ApplicationContext context: Context): InputMethodManager =
        context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    @Named("licenses")
    fun provideLicenses(@ApplicationContext context: Context): List<String> = listOf(
        context.getString(R.string.license_name_cc0),
        context.getString(R.string.license_name_cc_by),
        context.getString(R.string.license_name_cc_by_sa),
        context.getString(R.string.license_name_cc_by_four),
        context.getString(R.string.license_name_cc_by_sa_four)
    )

    @Provides
    @Named("licenses_by_name")
    fun provideLicensesByName(@ApplicationContext context: Context): Map<String, String> = mapOf(
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
    fun provideCategoryContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.CATEGORY_AUTHORITY)

    @Provides
    @Named("recentsearch")
    fun provideRecentSearchContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.RECENT_SEARCH_AUTHORITY)

    @Provides
    @Named("contribution")
    fun provideContributionContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.CONTRIBUTION_AUTHORITY)

    @Provides
    @Named("modification")
    fun provideModificationContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.MODIFICATION_AUTHORITY)

    @Provides
    @Named("bookmarks")
    fun provideBookmarkContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.BOOKMARK_AUTHORITY)

    @Provides
    @Named("bookmarksItem")
    fun provideBookmarkItemContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.BOOKMARK_ITEMS_AUTHORITY)

    /**
     * This method is used to provide instance of RecentLanguagesContentProvider
     * which provides content of recent used languages from database
     * @param context Context
     * @return returns RecentLanguagesContentProvider
     */
    @Provides
    @Named("recent_languages")
    fun provideRecentLanguagesContentProviderClient(@ApplicationContext context: Context): ContentProviderClient? =
        context.contentResolver.acquireContentProviderClient(BuildConfig.RECENT_LANGUAGE_AUTHORITY)

    /**
     * Provides a Json store instance(JsonKvStore) which keeps
     * the provided Gson in it's instance
     * @param gson stored inside the store instance
     */
    @Provides
    @Named("default_preferences")
    fun providesDefaultKvStore(@ApplicationContext context: Context, gson: Gson): JsonKvStore =
        JsonKvStore(context, "${context.packageName}_preferences", gson)

    @Provides
    fun providesUploadController(
        sessionManager: SessionManager,
        @Named("default_preferences") kvStore: JsonKvStore,
        @ApplicationContext context: Context
    ): UploadController = UploadController(sessionManager, context, kvStore)

    @Provides
    @Singleton
    fun provideLocationServiceManager(@ApplicationContext context: Context): LocationServiceManager =
        LocationServiceManager(context)

    @Provides
    @Singleton
    fun provideDBOpenHelper(@ApplicationContext context: Context): DBOpenHelper =
        DBOpenHelper(context)

    @Provides
    @Singleton
    @Named("thumbnail-cache")
    fun provideLruCache(): LruCache<String, String> =
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
    fun provideAppDataBase(@ApplicationContext context: Context): AppDatabase = databaseBuilder(
        context,
        AppDatabase::class.java,
        "commons_room.db"
    ).addMigrations(
        MIGRATION_1_2,
        MIGRATION_19_TO_20
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun providesContributionsDao(appDatabase: AppDatabase): ContributionDao =
        appDatabase.contributionDao()

    @Provides
    fun providesPlaceDao(appDatabase: AppDatabase): PlaceDao =
        appDatabase.PlaceDao()

    @Provides
    fun providesBookmarkLocationsDao(appDatabase: AppDatabase): BookmarkLocationsDao =
        appDatabase.bookmarkLocationsDao()

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
    fun providesBookmarkCategoriesDao (appDatabase: AppDatabase): BookmarkCategoriesDao =
        appDatabase.bookmarkCategoriesDao()

    @Provides
    fun providesContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    fun provideTimeProvider(): TimeProvider {
        return TimeProvider(System::currentTimeMillis)
    }

    const val IO_THREAD: String = "io_thread"
    const val MAIN_THREAD: String = "main_thread"

    lateinit var appContext: Context
        private set

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contribution " + " ADD COLUMN hasInvalidLocation INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_19_TO_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS bookmarks_locations (
                    location_name TEXT NOT NULL PRIMARY KEY,
                    location_language TEXT NOT NULL,
                    location_description TEXT NOT NULL,
                    location_lat REAL NOT NULL,
                    location_long REAL NOT NULL,
                    location_category TEXT NOT NULL,
                    location_label_text TEXT NOT NULL,
                    location_label_icon INTEGER,
                    location_image_url TEXT NOT NULL DEFAULT '',
                    location_wikipedia_link TEXT NOT NULL,
                    location_wikidata_link TEXT NOT NULL,
                    location_commons_link TEXT NOT NULL,
                    location_pic TEXT NOT NULL,
                    location_exists INTEGER NOT NULL CHECK(location_exists IN (0, 1))
                )
            """
                )

                val oldDbPath = appContext.getDatabasePath("commons.db").path
                val oldDb = SQLiteDatabase
                    .openDatabase(oldDbPath, null, SQLiteDatabase.OPEN_READONLY)

                val cursor = oldDb.rawQuery("SELECT * FROM bookmarksLocations", null)

                while (cursor.moveToNext()) {
                    val locationName =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_name"))
                    val locationLanguage =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_language"))
                    val locationDescription =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_description"))
                    val locationCategory =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_category"))
                    val locationLabelText =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_label_text"))
                    val locationLabelIcon =
                        cursor.getInt(cursor.getColumnIndexOrThrow("location_label_icon"))
                    val locationLat =
                        cursor.getDouble(cursor.getColumnIndexOrThrow("location_lat"))
                    val locationLong =
                        cursor.getDouble(cursor.getColumnIndexOrThrow("location_long"))

                    // Handle NULL values safely
                    val locationImageUrl =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_image_url")
                        ) ?: ""
                    val locationWikipediaLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_wikipedia_link")
                        ) ?: ""
                    val locationWikidataLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_wikidata_link")
                        ) ?: ""
                    val locationCommonsLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_commons_link")
                        ) ?: ""
                    val locationPic =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_pic")
                        ) ?: ""
                    val locationExists =
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow("location_exists")
                        )

                    db.execSQL(
                        """
                    INSERT OR REPLACE INTO bookmarks_locations (
                        location_name, location_language, location_description, location_category,
                        location_label_text, location_label_icon, location_lat, location_long,
                        location_image_url, location_wikipedia_link, location_wikidata_link,
                        location_commons_link, location_pic, location_exists
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                        arrayOf(
                            locationName, locationLanguage, locationDescription, locationCategory,
                            locationLabelText, locationLabelIcon, locationLat, locationLong,
                            locationImageUrl, locationWikipediaLink, locationWikidataLink,
                            locationCommonsLink, locationPic, locationExists
                        )
                    )
                }

                cursor.close()
                oldDb.close()
            }
        }
}
