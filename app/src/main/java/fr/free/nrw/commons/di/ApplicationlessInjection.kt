package fr.free.nrw.commons.di

import android.app.Activity
import android.app.Fragment
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Context
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasAndroidInjector
import dagger.android.HasBroadcastReceiverInjector
import dagger.android.HasContentProviderInjector
import dagger.android.HasFragmentInjector
import dagger.android.HasServiceInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject
import androidx.fragment.app.Fragment as AndroidXFragmen

/**
 * Provides injectors for all sorts of components
 * Ex: Activities, Fragments, Services, ContentProviders
 */
class ApplicationlessInjection(applicationContext: Context) : HasAndroidInjector,
    HasActivityInjector, HasFragmentInjector, HasSupportFragmentInjector, HasServiceInjector,
    HasBroadcastReceiverInjector, HasContentProviderInjector {
    @Inject @JvmField
    var androidInjector: DispatchingAndroidInjector<Any>? = null

    @Inject @JvmField
    var activityInjector: DispatchingAndroidInjector<Activity>? = null

    @Inject @JvmField
    var broadcastReceiverInjector: DispatchingAndroidInjector<BroadcastReceiver>? = null

    @Inject @JvmField
    var fragmentInjector: DispatchingAndroidInjector<Fragment>? = null

    @Inject @JvmField
    var supportFragmentInjector: DispatchingAndroidInjector<AndroidXFragmen>? = null

    @Inject @JvmField
    var serviceInjector: DispatchingAndroidInjector<Service>? = null

    @Inject @JvmField
    var contentProviderInjector: DispatchingAndroidInjector<ContentProvider>? = null

    val instance: ApplicationlessInjection get() = _instance!!

    val commonsApplicationComponent: CommonsApplicationComponent =
        DaggerCommonsApplicationComponent
            .builder()
            .appModule(CommonsApplicationModule(applicationContext))
            .build()

    init {
        commonsApplicationComponent.inject(this)
    }

    override fun androidInjector(): AndroidInjector<Any>? =
        androidInjector

    override fun activityInjector(): DispatchingAndroidInjector<Activity>? =
        activityInjector

    override fun fragmentInjector(): DispatchingAndroidInjector<Fragment>? =
        fragmentInjector

    override fun supportFragmentInjector(): DispatchingAndroidInjector<AndroidXFragmen>? =
        supportFragmentInjector

    override fun broadcastReceiverInjector(): DispatchingAndroidInjector<BroadcastReceiver>? =
        broadcastReceiverInjector

    override fun serviceInjector(): DispatchingAndroidInjector<Service>? =
        serviceInjector

    override fun contentProviderInjector(): AndroidInjector<ContentProvider>? =
        contentProviderInjector

    companion object {
        private var _instance: ApplicationlessInjection? = null

        @JvmStatic
        fun getInstance(applicationContext: Context): ApplicationlessInjection {
            if (_instance == null) {
                synchronized(ApplicationlessInjection::class.java) {
                    if (_instance == null) {
                        _instance = ApplicationlessInjection(applicationContext)
                    }
                }
            }

            return _instance!!
        }
    }
}
