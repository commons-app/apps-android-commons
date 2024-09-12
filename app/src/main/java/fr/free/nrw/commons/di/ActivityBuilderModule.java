package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SignupActivity;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity;
import fr.free.nrw.commons.description.DescriptionEditActivity;
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.media.ZoomableActivity;
import fr.free.nrw.commons.nearby.WikidataFeedback;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadProgressActivity;

/**
 * This Class handles the dependency injection (using dagger)
 * so, if a developer needs to add a new activity to the commons app
 * then that must be mentioned here to inject the dependencies
 */
@Module
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ActivityBuilderModule {

    @ContributesAndroidInjector
    abstract LoginActivity bindLoginActivity();

    @ContributesAndroidInjector
    abstract WelcomeActivity bindWelcomeActivity();

    @ContributesAndroidInjector
    abstract MainActivity bindContributionsActivity();

    @ContributesAndroidInjector
    abstract CustomSelectorActivity bindCustomSelectorActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity bindSettingsActivity();

    @ContributesAndroidInjector
    abstract AboutActivity bindAboutActivity();

    @ContributesAndroidInjector
    abstract LocationPickerActivity bindLocationPickerActivity();

    @ContributesAndroidInjector
    abstract SignupActivity bindSignupActivity();

    @ContributesAndroidInjector
    abstract NotificationActivity bindNotificationActivity();

    @ContributesAndroidInjector
    abstract UploadActivity bindUploadActivity();

    @ContributesAndroidInjector
    abstract SearchActivity bindSearchActivity();

    @ContributesAndroidInjector
    abstract CategoryDetailsActivity bindCategoryDetailsActivity();

    @ContributesAndroidInjector
    abstract WikidataItemDetailsActivity bindDepictionDetailsActivity();

    @ContributesAndroidInjector
    abstract ProfileActivity bindAchievementsActivity();

    @ContributesAndroidInjector
    abstract ReviewActivity bindReviewActivity();

    @ContributesAndroidInjector
    abstract DescriptionEditActivity bindDescriptionEditActivity();

    @ContributesAndroidInjector
    abstract ZoomableActivity bindZoomableActivity();

    @ContributesAndroidInjector
    abstract UploadProgressActivity bindUploadProgressActivity();

    @ContributesAndroidInjector
    abstract WikidataFeedback bindWikiFeedback();
}
