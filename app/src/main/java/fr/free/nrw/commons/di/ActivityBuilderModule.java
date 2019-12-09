package fr.free.nrw.commons.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.achievements.AchievementsActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.auth.SignupActivity;
import fr.free.nrw.commons.bookmarks.BookmarksActivity;
import fr.free.nrw.commons.category.CategoryDetailsActivity;
import fr.free.nrw.commons.category.CategoryImagesActivity;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.explore.SearchActivity;
import fr.free.nrw.commons.explore.categories.ExploreActivity;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.review.ReviewActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import fr.free.nrw.commons.upload.UploadActivity;

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
    abstract SettingsActivity bindSettingsActivity();

    @ContributesAndroidInjector
    abstract AboutActivity bindAboutActivity();

    @ContributesAndroidInjector
    abstract SignupActivity bindSignupActivity();

    @ContributesAndroidInjector
    abstract NotificationActivity bindNotificationActivity();

    @ContributesAndroidInjector
    abstract CategoryImagesActivity bindFeaturedImagesActivity();

    @ContributesAndroidInjector
    abstract UploadActivity bindUploadActivity();

    @ContributesAndroidInjector
    abstract SearchActivity bindSearchActivity();

    @ContributesAndroidInjector
    abstract CategoryDetailsActivity bindCategoryDetailsActivity();

    @ContributesAndroidInjector
    abstract ExploreActivity bindExploreActivity();

    @ContributesAndroidInjector
    abstract AchievementsActivity bindAchievementsActivity();

    @ContributesAndroidInjector
    abstract BookmarksActivity bindBookmarksActivity();

    @ContributesAndroidInjector
    abstract ReviewActivity bindReviewActivity();
}
