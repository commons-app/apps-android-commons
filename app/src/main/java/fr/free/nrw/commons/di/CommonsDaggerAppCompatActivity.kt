package fr.free.nrw.commons.di

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance
import javax.inject.Inject

abstract class CommonsDaggerAppCompatActivity : AppCompatActivity(), HasSupportFragmentInjector {
    @Inject @JvmField
    var supportFragmentInjector: DispatchingAndroidInjector<Fragment>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        inject()
        super.onCreate(savedInstanceState)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return supportFragmentInjector!!
    }

    /**
     * when this Activity is created it injects an instance of this class inside
     * activityInjector method of ApplicationlessInjection
     */
    private fun inject() {
        val injection = getInstance(applicationContext)

        val activityInjector = injection.activityInjector()
            ?: throw NullPointerException("ApplicationlessInjection.activityInjector() returned null")

        activityInjector.inject(this)
    }
}
