package fr.free.nrw.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.util.LruCache;

import com.squareup.leakcanary.RefWatcher;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsApplicationComponent;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.di.DaggerCommonsApplicationComponent;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.upload.UploadController;

public class TestCommonsApplication extends CommonsApplication {

    CommonsApplicationComponent mockApplicationComponent;

    @Mock
    CommonsApplicationModule commonsApplicationModule;
    @Mock
    AccountUtil accountUtil;
    @Mock
    SharedPreferences appSharedPreferences;
    @Mock
    SharedPreferences defaultSharedPreferences;
    @Mock
    SharedPreferences otherSharedPreferences;
    @Mock
    UploadController uploadController;
    @Mock
    SessionManager sessionManager;
    @Mock
    MediaWikiApi mediaWikiApi;
    @Mock
    LocationServiceManager locationServiceManager;
    @Mock
    CacheController cacheController;
    @Mock
    DBOpenHelper dbOpenHelper;
    @Mock
    NearbyPlaces nearbyPlaces;
    @Mock
    LruCache<String, String> lruCache;

    @Override
    public void onCreate() {
        MockitoAnnotations.initMocks(this);
        if (mockApplicationComponent == null) {
            mockApplicationComponent = DaggerCommonsApplicationComponent.builder()
                    .appModule(new CommonsApplicationModule(this) {
                        @Override
                        public AccountUtil providesAccountUtil(Context context) {
                            return accountUtil;
                        }

                        @Override
                        public SharedPreferences providesApplicationSharedPreferences(Context context) {
                            return appSharedPreferences;
                        }

                        @Override
                        public SharedPreferences providesDefaultSharedPreferences(Context context) {
                            return defaultSharedPreferences;
                        }

                        @Override
                        public SharedPreferences providesOtherSharedPreferences(Context context) {
                            return otherSharedPreferences;
                        }

                        @Override
                        public UploadController providesUploadController(Context context, SessionManager sessionManager, SharedPreferences sharedPreferences) {
                            return uploadController;
                        }

                        @Override
                        public SessionManager providesSessionManager(Context context, MediaWikiApi mediaWikiApi) {
                            return sessionManager;
                        }

                        @Override
                        public MediaWikiApi provideMediaWikiApi(Context context) {
                            return mediaWikiApi;
                        }

                        @Override
                        public LocationServiceManager provideLocationServiceManager(Context context) {
                            return locationServiceManager;
                        }

                        @Override
                        public CacheController provideCacheController() {
                            return cacheController;
                        }

                        @Override
                        public DBOpenHelper provideDBOpenHelper(Context context) {
                            return dbOpenHelper;
                        }

                        @Override
                        public NearbyPlaces provideNearbyPlaces() {
                            return nearbyPlaces;
                        }

                        @Override
                        public LruCache<String, String> provideLruCache() {
                            return lruCache;
                        }
                    }).build();
        }
        super.onCreate();
    }

    @Override
    protected RefWatcher setupLeakCanary() {
        // No leakcanary in unit tests.
        return RefWatcher.DISABLED;
    }

    public AccountUtil getAccountUtil() {
        return accountUtil;
    }

    public SharedPreferences getAppSharedPreferences() {
        return appSharedPreferences;
    }

    public SharedPreferences getDefaultSharedPreferences() {
        return defaultSharedPreferences;
    }

    public SharedPreferences getOtherSharedPreferences() {
        return otherSharedPreferences;
    }

    public UploadController getUploadController() {
        return uploadController;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public MediaWikiApi getMediaWikiApi() {
        return mediaWikiApi;
    }

    public LocationServiceManager getLocationServiceManager() {
        return locationServiceManager;
    }

    public CacheController getCacheController() {
        return cacheController;
    }

    public DBOpenHelper getDbOpenHelper() {
        return dbOpenHelper;
    }

    public NearbyPlaces getNearbyPlaces() {
        return nearbyPlaces;
    }

    public LruCache<String, String> getLruCache() {
        return lruCache;
    }
}
