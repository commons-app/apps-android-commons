package fr.free.nrw.commons.di;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.contributions.ContributionViewHolder;
import fr.free.nrw.commons.contributions.ContributionsSyncAdapter;
import fr.free.nrw.commons.modifications.ModificationsSyncAdapter;
import fr.free.nrw.commons.nearby.PlaceRenderer;
import fr.free.nrw.commons.review.ReviewController;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.upload.FileProcessor;
import fr.free.nrw.commons.widget.PicOfDayAppWidget;


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

    void inject(LoginActivity activity);

    void inject(SettingsFragment fragment);

    void inject(ReviewController reviewController);

    @Override
    void inject(ApplicationlessInjection instance);

    void inject(PlaceRenderer placeRenderer);

    void inject(FileProcessor fileProcessor);

    void inject(PicOfDayAppWidget picOfDayAppWidget);

    void inject(ContributionViewHolder viewHolder);

    @Component.Builder
    @SuppressWarnings({"WeakerAccess", "unused"})
    interface Builder {
        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
