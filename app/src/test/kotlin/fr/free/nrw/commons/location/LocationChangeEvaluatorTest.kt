package fr.free.nrw.commons.location

import android.location.Location
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LocationChangeEvaluator].
 */
class LocationChangeEvaluatorTest {
    private val evaluator = LocationChangeEvaluator()

    /**
     * Verifies that evaluating a new location without a known current location returns a significant change.
     */
    @Test
    fun evaluateWithoutCurrentLocationReturnsSignificantChange() {
        val location = Location("gps")

        assertEquals(
            LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED,
            evaluator.evaluate(location, null),
        )
    }

    /**
     * Verifies that evaluating an identical location update returns no change.
     */
    @Test
    fun evaluateUnchangedLocationReturnsNoChange() {
        val currentLocation =
            Location("gps").apply {
                latitude = 52.0
                longitude = 13.0
                accuracy = 10f
                time = 1000L
            }
        val newLocation = Location(currentLocation)

        assertEquals(
            LocationChangeType.LOCATION_NOT_CHANGED,
            evaluator.evaluate(newLocation, currentLocation),
        )
    }
}
