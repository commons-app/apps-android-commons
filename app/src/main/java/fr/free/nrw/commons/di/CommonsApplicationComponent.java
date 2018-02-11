package fr.free.nrw.commons.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.category.CategoryContentProvider;
import fr.free.nrw.commons.contributions.ContributionsContentProvider;
import fr.free.nrw.commons.contributions.ContributionsSyncAdapter;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModificationsSyncAdapter;
import fr.free.nrw.commons.settings.SettingsFragment;

@Singleton
@Component(modules = {
        CommonsApplicationModule.class,
        AndroidInjectionModule.class,
        AndroidSupportInjectionModule.class,
        ActivityBuilderModule.class,
        FragmentBuilderModule.class,
        ServiceBuilderModule.class,
        ContentProviderBuilderModule.class
})
public interface CommonsApplicationComponent extends AndroidInjector<ApplicationlessInjection> {
    void inject(CommonsApplication application);

    void inject(ContributionsSyncAdapter syncAdapter);

    void inject(ModificationsSyncAdapter syncAdapter);

    void inject(MediaWikiImageView mediaWikiImageView);

    void inject(LoginActivity activity);

    void inject(SettingsFragment fragment);

    @Override
    void inject(ApplicationlessInjection instance);

    @Component.Builder
    @SuppressWarnings({"WeakerAccess", "unused"})
    interface Builder {
        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
