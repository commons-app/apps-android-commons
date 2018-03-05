package fr.free.nrw.commons.di;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.upload.UploadController;

import static android.content.Context.MODE_PRIVATE;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.CONTRIBUTION_AUTHORITY;
import static fr.free.nrw.commons.modifications.ModificationsContentProvider.MODIFICATIONS_AUTHORITY;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class CommonsApplicationModule {
    public static final String CATEGORY_AUTHORITY = "fr.free.nrw.commons.categories.contentprovider";
    public static final long OK_HTTP_CACHE_SIZE = 10 * 1024 * 1024;

    private Context applicationContext;

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
    public AccountUtil providesAccountUtil(Context context) {
        return new AccountUtil(context);
    }

    @Provides
    @Named("category")
    public ContentProviderClient provideCategoryContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(CATEGORY_AUTHORITY);
    }

    @Provides
    @Named("contribution")
    public ContentProviderClient provideContributionContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(CONTRIBUTION_AUTHORITY);
    }

    @Provides
    @Named("modification")
    public ContentProviderClient provideModificationContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(MODIFICATIONS_AUTHORITY);
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
    public MediaWikiApi provideMediaWikiApi(Context context,
                                            @Named("default_preferences") SharedPreferences defaultPreferences,
                                            @Named("category_prefs") SharedPreferences categoryPrefs,
                                            Gson gson) {
        return new ApacheHttpClientMediaWikiApi(context, BuildConfig.WIKIMEDIA_API_HOST, defaultPreferences, categoryPrefs, gson);
    }

    @Provides
    @Singleton
    public LocationServiceManager provideLocationServiceManager(Context context) {
        return new LocationServiceManager(context);
    }

    /**
     * Gson objects are very heavy. The app should ideally be using just one instance of it instead of creating new instances everywhere.
     * @return returns a singleton Gson instance
     */
    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public CacheController provideCacheController() {
        return new CacheController();
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
}