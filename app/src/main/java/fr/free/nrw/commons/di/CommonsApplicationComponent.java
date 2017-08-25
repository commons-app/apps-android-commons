package fr.free.nrw.commons.di;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.WikiAccountAuthenticatorService;
import fr.free.nrw.commons.contributions.ContributionsSyncAdapter;
import fr.free.nrw.commons.modifications.ModificationsSyncAdapter;

@Singleton
@Component(modules = {
        CommonsApplicationModule.class,
        AndroidInjectionModule.class,
        ActivityBuilderModule.class,
        ContentProviderBuilderModule.class
})
public interface CommonsApplicationComponent extends AndroidInjector<CommonsApplication> {
    void inject(CommonsApplication application);

    void inject(WikiAccountAuthenticatorService service);

    void inject(ContributionsSyncAdapter syncAdapter);

    void inject(ModificationsSyncAdapter syncAdapter);

    @Component.Builder
    interface Builder {
        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
