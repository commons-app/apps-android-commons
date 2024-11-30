package fr.free.nrw.commons.di

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.free.nrw.commons.di.ApplicationlessInjection.Companion.getInstance

/**
 * Receives broadcast then injects it's instance to the broadcastReceiverInjector method of
 * ApplicationlessInjection class
 */
abstract class CommonsDaggerBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        inject(context)
    }

    private fun inject(context: Context) {
        val injection = getInstance(context.applicationContext)

        val serviceInjector = injection.broadcastReceiverInjector()
                ?: throw NullPointerException("ApplicationlessInjection.broadcastReceiverInjector() returned null")

        serviceInjector.inject(this)
    }
}
