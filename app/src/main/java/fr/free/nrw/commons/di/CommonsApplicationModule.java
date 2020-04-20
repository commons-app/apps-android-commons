package fr.free.nrw.commons.di;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import androidx.collection.LruCache;
import androidx.room.Room;
import androidx.room.RoomDatabase.Builder;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.github.varunpant.quadtree.QuadTree;
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.db.AppDatabase;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadController;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import fr.free.nrw.commons.wikidata.WikidataEditListenerImpl;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Named;
import javax.inject.Singleton;
import org.wikipedia.AppAdapter;

/**
 * The Dependency Provider class for Commons Android.
 *
 * Provides all sorts of ContentProviderClients used by the app
 * along with the Liscences, AccountUtility, UploadController, Logged User,
 * Location manager etc
 */
@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class CommonsApplicationModule {
    private Context applicationContext;
    public static final String IO_THREAD="io_thread";
    public static final String MAIN_THREAD="main_thread";
    private AppDatabase appDatabase;

    public CommonsApplicationModule(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Provides
    public Context providesApplicationContext() {
        return this.applicationContext;
    }

    @Provides
    public InputMethodManager provideInputMethodManager() {
        return (InputMethodManager) applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
    }

    @Provides
    @Named("licenses")
    public List<String> provideLicenses(Context context) {
        List<String> licenseItems = new ArrayList<>();
        licenseItems.add(context.getString(R.string.license_name_cc0));
        licenseItems.add(context.getString(R.string.license_name_cc_by));
        licenseItems.add(context.getString(R.string.license_name_cc_by_sa));
        licenseItems.add(context.getString(R.string.license_name_cc_by_four));
        licenseItems.add(context.getString(R.string.license_name_cc_by_sa_four));
        return licenseItems;
    }

    @Provides
    @Named("licenses_by_name")
    public Map<String, String> provideLicensesByName(Context context) {
        Map<String, String> byName = new HashMap<>();
        byName.put(context.getString(R.string.license_name_cc0), Prefs.Licenses.CC0);
        byName.put(context.getString(R.string.license_name_cc_by), Prefs.Licenses.CC_BY_3);
        byName.put(context.getString(R.string.license_name_cc_by_sa), Prefs.Licenses.CC_BY_SA_3);
        byName.put(context.getString(R.string.license_name_cc_by_four), Prefs.Licenses.CC_BY_4);
        byName.put(context.getString(R.string.license_name_cc_by_sa_four), Prefs.Licenses.CC_BY_SA_4);
        return byName;
    }

    @Provides
    public AccountUtil providesAccountUtil(Context context) {
        return new AccountUtil();
    }

    /**
     * Provides an instance of CategoryContentProviderClient i.e. the categories
     * that are there in local storage
     */
    @Provides
    @Named("category")
    public ContentProviderClient provideCategoryContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.CATEGORY_AUTHORITY);
    }

    /**
     * This method is used to provide instance of RecentSearchContentProviderClient
     * which provides content of Recent Searches from database
     * @param context
     * @return returns RecentSearchContentProviderClient
     */
    @Provides
    @Named("recentsearch")
    public ContentProviderClient provideRecentSearchContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.RECENT_SEARCH_AUTHORITY);
    }

    @Provides
    @Named("contribution")
    public ContentProviderClient provideContributionContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.CONTRIBUTION_AUTHORITY);
    }

    @Provides
    @Named("modification")
    public ContentProviderClient provideModificationContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.MODIFICATION_AUTHORITY);
    }

    @Provides
    @Named("bookmarks")
    public ContentProviderClient provideBookmarkContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.BOOKMARK_AUTHORITY);
    }

    @Provides
    @Named("bookmarksLocation")
    public ContentProviderClient provideBookmarkLocationContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(BuildConfig.BOOKMARK_LOCATIONS_AUTHORITY);
    }

    /**
     * Provides a Json store instance(JsonKvStore) which keeps
     * the provided Gson in it's instance
     * @param gson stored inside the store instance
     */
    @Provides
    @Named("default_preferences")
    public JsonKvStore providesDefaultKvStore(Context context, Gson gson) {
        String storeName = context.getPackageName() + "_preferences";
        return new JsonKvStore(context, storeName, gson);
    }

    @Provides
    public UploadController providesUploadController(SessionManager sessionManager,
                                                     @Named("default_preferences") JsonKvStore kvStore,
                                                     Context context) {
        return new UploadController(sessionManager, context, kvStore);
    }

    @Provides
    @Singleton
    public LocationServiceManager provideLocationServiceManager(Context context) {
        return new LocationServiceManager(context);
    }

    @Provides
    @Singleton
    public DBOpenHelper provideDBOpenHelper(Context context) {
        return new DBOpenHelper(context);
    }

    @Provides
    @Singleton
    @Named("thumbnail-cache")
    public LruCache<String, String> provideLruCache() {
        return new LruCache<>(1024);
    }

    @Provides
    @Singleton
    public WikidataEditListener provideWikidataEditListener() {
        return new WikidataEditListenerImpl();
    }

    /**
     * Provides app flavour. Can be used to alter flows in the app
     * @return
     */
    @Named("isBeta")
    @Provides
    @Singleton
    public boolean provideIsBetaVariant() {
        return ConfigUtils.isBetaFlavour();
    }

    /**
     * Provide JavaRx IO scheduler which manages IO operations
     * across various Threads
     */
    @Named(IO_THREAD)
    @Provides
    public Scheduler providesIoThread(){
        return Schedulers.io();
    }

    @Named(MAIN_THREAD)
    @Provides
    public Scheduler providesMainThread() {
        return AndroidSchedulers.mainThread();
    }

    @Named("username")
    @Provides
    public String provideLoggedInUsername() {
        return Objects.toString(AppAdapter.get().getUserName(), "");
    }

    /**
     * Provides quad tree
     *
     * @return
     */
    @Provides
    public QuadTree providesQuadTres() {
        return new QuadTree<>(-180, -90, +180, +90);
    }

    @Provides
    @Singleton
    public AppDatabase provideAppDataBase() {
        Builder<AppDatabase> appDatabaseBuilder = Room
            .databaseBuilder(applicationContext, AppDatabase.class, "commons_room.db");
        appDatabaseBuilder.addMigrations(MIGRATION_1_2);
        appDatabase = appDatabaseBuilder.build();
        return appDatabase;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE `contribution`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `contribution` ("
                + "`localUri` TEXT,"
                + " `thumbUrl` TEXT,"
                + " `imageUrl` TEXT,"
                + " `filename` TEXT,"
                + " `description` TEXT,"
                + " `discussion` TEXT,"
                + " `dateCreated` INTEGER,"
                + " `dateUploaded` INTEGER,"
                + " `width` INTEGER NOT NULL,"
                + " `height` INTEGER NOT NULL,"
                + " `license` TEXT,"
                + " `licenseUrl` TEXT,"
                + " `creator` TEXT,"
                + " `categories` TEXT,"
                + " `requestedDeletion`"
                + " INTEGER NOT NULL,"
                + " `descriptions` TEXT,"
                + " `tags` TEXT,"
                + " `coordinates` TEXT,"
                + " `pageId` INTEGER NOT NULL,"
                + " `contentUri` TEXT,"
                + " `source` TEXT,"
                + " `editSummary` TEXT,"
                + " `state` INTEGER NOT NULL,"
                + " `transferred` INTEGER NOT NULL,"
                + " `decimalCoords` TEXT,"
                + " `isMultiple` INTEGER NOT NULL,"
                + " `wikiDataEntityId` TEXT,"
                + " `wikiItemName` TEXT,"
                + " `p18Value` TEXT,"
                + " `contentProviderUri` TEXT,"
                + " `dateCreatedSource` TEXT,"
                + " `dataLength` INTEGER NOT NULL,"
                + " PRIMARY KEY(`pageId`))");
        }
    };

    @Provides
    public ContributionDao providesContributionsDao() {
        return appDatabase.getContributionDao();
    }

    @Provides
    public ContentResolver providesContentResolver(Context context){
        return context.getContentResolver();
    }
}
