package fr.free.nrw.commons.di;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MediaWikiImageView;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.contributions.ContributionsSyncAdapter;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.modifications.ModificationsSyncAdapter;
import fr.free.nrw.commons.nearby.PlaceRenderer;
import fr.free.nrw.commons.upload.FileProcessor;
import fr.free.nrw.commons.settings.SettingsFragment;


@Singleton
@Component(modules = {
        CommonsApplicationModule.class,
        NetworkingModule.class,
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

    void inject(DeleteTask deleteTask);

    void inject(SettingsFragment fragment);

    @Override
    void inject(ApplicationlessInjection instance);

    void inject(PlaceRenderer placeRenderer);

    void inject(FileProcessor fileProcessor);

    @Component.Builder
    @SuppressWarnings({"WeakerAccess", "unused"})
    interface Builder {
        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
