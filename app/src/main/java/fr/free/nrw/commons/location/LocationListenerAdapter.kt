package fr.free.nrw.commons.location

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import timber.log.Timber

/**
 * Provides the default logging callbacks for a location listener.
 */
open class LocationListenerAdapter : LocationListener {
    /**
     * Receives a location update.
     *
     * @param location the updated location
     */
    override fun onLocationChanged(location: Location) = Unit

    /**
     * Logs a provider status change.
     *
     * @param provider the provider whose status changed
     * @param status the new provider status
     * @param extras additional provider status data
     */
    @Deprecated(
        "Deprecated in Java",
        ReplaceWith(
            "Timber.d(\"%s's status changed to %d\", provider, status)",
            "timber.log.Timber",
        ),
    )
    override fun onStatusChanged(
        provider: String?,
        status: Int,
        extras: Bundle?,
    ) {
        Timber.d("%s's status changed to %d", provider, status)
    }

    /**
     * Logs that a provider became enabled.
     *
     * @param provider the enabled provider
     */
    override fun onProviderEnabled(provider: String) {
        Timber.d("Provider %s enabled", provider)
    }

    /**
     * Logs that a provider became disabled.
     *
     * @param provider the disabled provider
     */
    override fun onProviderDisabled(provider: String) {
        Timber.d("Provider %s disabled", provider)
    }
}
