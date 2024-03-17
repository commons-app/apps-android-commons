package fr.free.nrw.commons.di;

import com.google.gson.Gson;

import fr.free.nrw.commons.explore.categories.CategoriesModule;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment;
import fr.free.nrw.commons.upload.worker.UploadWorker;
import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.contributions.ContributionsModule;
import fr.free.nrw.commons.explore.depictions.DepictionModule;
import fr.free.nrw.commons.explore.SearchModule;
import fr.free.nrw.commons.review.ReviewController;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.upload.FileProcessor;
import fr.free.nrw.commons.upload.UploadModule;
import fr.free.nrw.commons.widget.PicOfDayAppWidget;


/**
 * Facilitates Injection from CommonsApplicationModule to all the 
 * classes seeking a dependency to be injected
 */
@Singleton
@Component(modules = {
        CommonsApplicationModule.class,
        NetworkingModule.class,
        AndroidInjectionModule.class,
        AndroidSupportInjectionModule.class,
        ActivityBuilderModule.class,
        FragmentBuilderModule.class,
        ServiceBuilderModule.class,
        ContentProviderBuilderModule.class,
        UploadModule.class,
        ContributionsModule.class,
        SearchModule.class,
        DepictionModule.class,
        CategoriesModule.class
})
public interface CommonsApplicationComponent extends AndroidInjector<ApplicationlessInjection> {
    void inject(CommonsApplication application);

    void inject(UploadWorker worker);

    void inject(LoginActivity activity);

    void inject(SettingsFragment fragment);

    void inject(MoreBottomSheetFragment fragment);

    void inject(MoreBottomSheetLoggedOutFragment fragment);

    void inject(ReviewController reviewController);

    //void inject(NavTabLayout view);

    @Override
    void inject(ApplicationlessInjection instance);

    void inject(FileProcessor fileProcessor);

    void inject(PicOfDayAppWidget picOfDayAppWidget);

    Gson gson();

    @Component.Builder
    @SuppressWarnings({"WeakerAccess", "unused"})
    interface Builder {

        Builder appModule(CommonsApplicationModule applicationModule);

        CommonsApplicationComponent build();
    }
}
