package fr.free.nrw.commons.di

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

abstract class CommonsDaggerSupportFragment : Fragment(), HasSupportFragmentInjector {

    @Inject @JvmField
    var childFragmentInjector: DispatchingAndroidInjector<Fragment>? = null

    // Removed @JvmField to allow overriding
    protected open var compositeDisposable: CompositeDisposable = CompositeDisposable()

    override fun onAttach(context: Context) {
        inject()
        super.onAttach(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> =
        childFragmentInjector!!


    fun inject() {
        val hasSupportFragmentInjector = findHasFragmentInjector()

        val fragmentInjector = hasSupportFragmentInjector.supportFragmentInjector()
            ?: throw NullPointerException(
                String.format(
                    "%s.supportFragmentInjector() returned null",
                    hasSupportFragmentInjector.javaClass.canonicalName
                )
            )

        fragmentInjector.inject(this)
    }

    private fun findHasFragmentInjector(): HasSupportFragmentInjector {
        var parentFragment: Fragment? = this

        while ((parentFragment!!.parentFragment.also { parentFragment = it }) != null) {
            if (parentFragment is HasSupportFragmentInjector) {
                return parentFragment as HasSupportFragmentInjector
            }
        }

        val activity: Activity = requireActivity()

        if (activity is HasSupportFragmentInjector) {
            return activity
        }

        return getInstance(activity.applicationContext)
    }

    // Ensure getContext() returns a non-null Context
    override fun getContext(): Context {
        return super.getContext() ?: throw IllegalStateException("Context is null")
    }
}
