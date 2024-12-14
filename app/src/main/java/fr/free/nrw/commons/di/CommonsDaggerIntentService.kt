package fr.free.nrw.commons.di

import android.app.IntentService
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance

abstract class CommonsDaggerIntentService(name: String?) : IntentService(name) {
    override fun onCreate() {
        inject()
        super.onCreate()
    }

    private fun inject() {
        val injection = getInstance(applicationContext)

        val serviceInjector = injection.serviceInjector()
            ?: throw NullPointerException("ApplicationlessInjection.serviceInjector() returned null")

        serviceInjector.inject(this)
    }
}
