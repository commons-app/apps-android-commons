package fr.free.nrw.commons.di

import fr.free.nrw.commons.AboutActivity
import fr.free.nrw.commons.locationpicker.LocationPickerActivity
import fr.free.nrw.commons.WelcomeActivity
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SignupActivity
import fr.free.nrw.commons.category.CategoryDetailsActivity
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.description.DescriptionEditActivity
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.depictions.WikidataItemDetailsActivity
import fr.free.nrw.commons.media.ZoomableActivity
import fr.free.nrw.commons.nearby.WikidataFeedback
import fr.free.nrw.commons.notification.NotificationActivity
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.review.ReviewActivity
import fr.free.nrw.commons.settings.SettingsActivity
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadProgressActivity

/**
 * This Class handles the dependency injection (using dagger)
 * so, if a developer needs to add a new activity to the commons app
 * then that must be mentioned here to inject the dependencies
 *
 * NOTE: This module is DEPRECATED with Hilt. Activities should use @AndroidEntryPoint instead.
 * This file is kept for reference but all functionality has been migrated to Hilt.
 * The @Module annotation has been removed to prevent Hilt build errors.
 */
@Suppress("unused")
abstract class ActivityBuilderModule {
    // All methods below are deprecated and non-functional
    // Activities should use @AndroidEntryPoint annotation instead

    /*
    @ContributesAndroidInjector
    abstract fun bindLoginActivity(): LoginActivity

    @ContributesAndroidInjector
    abstract fun bindWelcomeActivity(): WelcomeActivity

    @ContributesAndroidInjector
    abstract fun bindContributionsActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindCustomSelectorActivity(): CustomSelectorActivity

    @ContributesAndroidInjector
    abstract fun bindSettingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    abstract fun bindAboutActivity(): AboutActivity

    @ContributesAndroidInjector
    abstract fun bindLocationPickerActivity(): LocationPickerActivity

    @ContributesAndroidInjector
    abstract fun bindSignupActivity(): SignupActivity

    @ContributesAndroidInjector
    abstract fun bindNotificationActivity(): NotificationActivity

    @ContributesAndroidInjector
    abstract fun bindUploadActivity(): UploadActivity

    @ContributesAndroidInjector
    abstract fun bindSearchActivity(): SearchActivity

    @ContributesAndroidInjector
    abstract fun bindCategoryDetailsActivity(): CategoryDetailsActivity

    @ContributesAndroidInjector
    abstract fun bindDepictionDetailsActivity(): WikidataItemDetailsActivity

    @ContributesAndroidInjector
    abstract fun bindAchievementsActivity(): ProfileActivity

    @ContributesAndroidInjector
    abstract fun bindReviewActivity(): ReviewActivity

    @ContributesAndroidInjector
    abstract fun bindDescriptionEditActivity(): DescriptionEditActivity

    @ContributesAndroidInjector
    abstract fun bindZoomableActivity(): ZoomableActivity

    @ContributesAndroidInjector
    abstract fun bindUploadProgressActivity(): UploadProgressActivity

    @ContributesAndroidInjector
    abstract fun bindWikiFeedback(): WikidataFeedback
    */
}
