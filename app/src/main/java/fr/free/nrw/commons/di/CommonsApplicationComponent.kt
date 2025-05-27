package fr.free.nrw.commons.di

import com.google.gson.Gson
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.activity.SingleWebViewActivity
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.contributions.ContributionsModule
import fr.free.nrw.commons.contributions.ContributionsProvidesModule
import fr.free.nrw.commons.explore.SearchModule
import fr.free.nrw.commons.explore.categories.CategoriesModule
import fr.free.nrw.commons.explore.depictions.DepictionModule
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.review.ReviewController
import fr.free.nrw.commons.settings.SettingsFragment
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.UploadModule
import fr.free.nrw.commons.upload.worker.UploadWorker
import fr.free.nrw.commons.widget.PicOfDayAppWidget
import javax.inject.Singleton

/**
 * Facilitates Injection from CommonsApplicationModule to all the
 * classes seeking a dependency to be injected
 */
@Singleton
@Component(
    modules = [
        CommonsApplicationModule::class,
        NetworkingModule::class,
        AndroidInjectionModule::class,
        AndroidSupportInjectionModule::class,
        ActivityBuilderModule::class,
        FragmentBuilderModule::class,
        ServiceBuilderModule::class,
        ContentProviderBuilderModule::class,
        UploadModule::class,
        ContributionsModule::class,
        ContributionsProvidesModule::class,
        SearchModule::class,
        DepictionModule::class,
        CategoriesModule::class
    ]
)
interface CommonsApplicationComponent : AndroidInjector<ApplicationlessInjection> {
    fun inject(application: CommonsApplication)

    fun inject(worker: UploadWorker)

    fun inject(activity: LoginActivity)

    fun inject(activity: SingleWebViewActivity)

    fun inject(fragment: SettingsFragment)

    fun inject(fragment: MoreBottomSheetFragment)

    fun inject(fragment: MoreBottomSheetLoggedOutFragment)

    fun inject(reviewController: ReviewController)

    override fun inject(instance: ApplicationlessInjection)

    fun inject(fileProcessor: FileProcessor)

    fun inject(picOfDayAppWidget: PicOfDayAppWidget)

    @Singleton
    fun inject(nearbyController: NearbyController)

    fun gson(): Gson

    @Component.Builder
    @Suppress("unused")
    interface Builder {
        fun appModule(applicationModule: CommonsApplicationModule): Builder

        fun build(): CommonsApplicationComponent
    }
}
