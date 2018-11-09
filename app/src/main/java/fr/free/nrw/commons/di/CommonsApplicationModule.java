package fr.free.nrw.commons.di;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.upload.UploadController;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import fr.free.nrw.commons.wikidata.WikidataEditListenerImpl;

import static android.content.Context.MODE_PRIVATE;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class CommonsApplicationModule {
    private Context applicationContext;

    public CommonsApplicationModule(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Provides
    public Context providesApplicationContext() {
        return this.applicationContext;
    }

    @Provides
    public AccountUtil providesAccountUtil(Context context) {
        return new AccountUtil(context);
    }

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

    @Provides
    @Named("application_preferences")
    public SharedPreferences providesApplicationSharedPreferences(Context context) {
        return context.getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
    }

    @Provides
    @Named("default_preferences")
    public SharedPreferences providesDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Named("prefs")
    public SharedPreferences providesOtherSharedPreferences(Context context) {
        return context.getSharedPreferences("prefs", MODE_PRIVATE);
    }

    /**
     *
     * @param context
     * @return returns categoryPrefs
     */
    @Provides
    @Named("category_prefs")
    public SharedPreferences providesCategorySharedPreferences(Context context) {
        return context.getSharedPreferences("categoryPrefs", MODE_PRIVATE);
    }

    @Provides
    @Named("direct_nearby_upload_prefs")
    public SharedPreferences providesDirectNearbyUploadPreferences(Context context) {
        return context.getSharedPreferences("direct_nearby_upload_prefs", MODE_PRIVATE);
    }

    @Provides
    public UploadController providesUploadController(SessionManager sessionManager, @Named("default_preferences") SharedPreferences sharedPreferences, Context context) {
        return new UploadController(sessionManager, context, sharedPreferences);
    }

    @Provides
    @Singleton
    public SessionManager providesSessionManager(Context context,
                                                 MediaWikiApi mediaWikiApi,
                                                 @Named("default_preferences") SharedPreferences sharedPreferences) {
        return new SessionManager(context, mediaWikiApi, sharedPreferences);
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
    public NearbyPlaces provideNearbyPlaces() {
        return new NearbyPlaces();
    }

    @Provides
    @Singleton
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
        return BuildConfig.FLAVOR.equals("beta");
    }
}
