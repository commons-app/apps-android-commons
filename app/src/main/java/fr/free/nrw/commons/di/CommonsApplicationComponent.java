package fr.free.nrw.commons.di;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService;
import fr.free.nrw.commons.contributions.ContributionsSyncAdapter;
import fr.free.nrw.commons.modifications.ModificationsSyncAdapter;
import fr.free.nrw.commons.upload.UploadService;

@Singleton
@Component(modules = {
        CommonsApplicationModule.class,
        AndroidInjectionModule.class,
        AndroidSupportInjectionModule.class,
        ActivityBuilderModule.class,
        ContentProviderBuilderModule.class
})
public interface CommonsApplicationComponent extends AndroidInjector<CommonsApplication> {
    void inject(CommonsApplication application);

    void inject(WikiAccountAuthenticatorService service);

    void inject(UploadService service);

    void inject(ContributionsSyncAdapter syncAdapter);

    void inject(ModificationsSyncAdapter syncAdapter);

    void inject(MediaWikiImageView mediaWikiImageView);

    @Component.Builder
    interface Builder {
        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
