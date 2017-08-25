package fr.free.nrw.commons.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

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
}
