package fr.free.nrw.commons.di;

import android.support.v4.util.LruCache;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;

@Module
public class CommonsApplicationModule {
    private CommonsApplication application;

    public CommonsApplicationModule(CommonsApplication application) {
        this.application = application;
    }

    @Provides
    public CommonsApplication providesCommonsApplication() {
        return application;
    }

    @Provides
    @Singleton
    public MediaWikiApi provideMediaWikiApi() {
        return new ApacheHttpClientMediaWikiApi(CommonsApplication.API_URL);
    }

    @Provides
    @Singleton
    public CacheController provideCacheController() {
        return new CacheController();
    }

    @Provides
    @Singleton
    public DBOpenHelper provideDBOpenHelper(CommonsApplication application) {
        return new DBOpenHelper(application);
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
