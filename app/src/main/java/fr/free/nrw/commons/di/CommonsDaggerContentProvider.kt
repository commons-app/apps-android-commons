package fr.free.nrw.commons.di

import android.content.ContentProvider
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance

abstract class CommonsDaggerContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        inject()
        return true
    }

    private fun inject() {
        val injection = getInstance(context!!)

        val serviceInjector = injection.contentProviderInjector()
            ?: throw NullPointerException("ApplicationlessInjection.contentProviderInjector() returned null")

        serviceInjector.inject(this)
    }
}
