package fr.free.nrw.commons.di;

import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.UploadController;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import fr.free.nrw.commons.wikidata.WikidataEditListenerImpl;

import static android.content.Context.MODE_PRIVATE;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.CONTRIBUTION_AUTHORITY;
import static fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider.RECENT_SEARCH_AUTHORITY;
import static fr.free.nrw.commons.modifications.ModificationsContentProvider.MODIFICATIONS_AUTHORITY;

@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public class CommonsApplicationModule {
    public static final String CATEGORY_AUTHORITY = "fr.free.nrw.commons.categories.contentprovider";

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
        return new AccountUtil(context);
    }

    @Provides
    @Named("category")
    public ContentProviderClient provideCategoryContentProviderClient(Context context) {
        return context.getContentResolver().acquireContentProviderClient(CATEGORY_AUTHORITY);
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
        return context.getContentResolver().acquireContentProviderClient(RECENT_SEARCH_AUTHORITY);
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

//    @Provides
//    @Singleton
//    public MediaWikiApi provideMediaWikiApi(Context context,
//                                            @Named("default_preferences") SharedPreferences defaultPreferences,
//                                            @Named("category_prefs") SharedPreferences categoryPrefs,
//                                            Gson gson) {
//        return new ApacheHttpClientMediaWikiApi(context, BuildConfig.WIKIMEDIA_API_HOST, defaultPreferences, categoryPrefs, gson);
//    }

    @Provides
    @Singleton
    public LocationServiceManager provideLocationServiceManager(Context context) {
        return new LocationServiceManager(context);
    }

    /*
     * Gson objects are very heavy. The app should ideally be using just one instance of it instead of creating new instances everywhere.
     * @return returns a singleton Gson instance
     */
//    @Provides
//    @Singleton
//    public Gson provideGson() {
//        return new Gson();
//    }
//
//    @Provides
//    @Singleton
//    public CacheController provideCacheController() {
//        return new CacheController();
//    }

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
}