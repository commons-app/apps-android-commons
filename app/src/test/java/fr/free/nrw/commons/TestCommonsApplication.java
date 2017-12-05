package fr.free.nrw.commons;

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
        super.onCreate();
    }

    @Override
    protected RefWatcher setupLeakCanary() {
        // No leakcanary in unit tests.
        return RefWatcher.DISABLED;
    }

    @Override
    public CommonsApplicationComponent injector() {
        if (mockApplicationComponent == null) {
            mockApplicationComponent = DaggerCommonsApplicationComponent.builder()
                    .appModule(new CommonsApplicationModule(this) {
                        @Override
                        public AccountUtil providesAccountUtil() {
                            return accountUtil;
                        }

                        @Override
                        public SharedPreferences providesApplicationSharedPreferences() {
                            return appSharedPreferences;
                        }

                        @Override
                        public SharedPreferences providesDefaultSharedPreferences() {
                            return defaultSharedPreferences;
                        }

                        @Override
                        public SharedPreferences providesOtherSharedPreferences() {
                            return otherSharedPreferences;
                        }

                        @Override
                        public UploadController providesUploadController(SessionManager sessionManager, SharedPreferences sharedPreferences) {
                            return uploadController;
                        }

                        @Override
                        public SessionManager providesSessionManager(MediaWikiApi mediaWikiApi) {
                            return sessionManager;
                        }

                        @Override
                        public MediaWikiApi provideMediaWikiApi() {
                            return mediaWikiApi;
                        }

                        @Override
                        public LocationServiceManager provideLocationServiceManager() {
                            return locationServiceManager;
                        }

                        @Override
                        public CacheController provideCacheController() {
                            return cacheController;
                        }

                        @Override
                        public DBOpenHelper provideDBOpenHelper() {
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
        return mockApplicationComponent;
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
