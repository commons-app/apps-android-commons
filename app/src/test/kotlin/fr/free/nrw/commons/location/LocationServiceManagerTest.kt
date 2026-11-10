package fr.free.nrw.commons.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test

class LocationServiceManagerTest {
    private val context = mock<Context>()
    private val locationManager = mock<LocationManager>()
    private lateinit var locationServiceManager: LocationServiceManager

    @Before
    fun setUp() {
        whenever(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        locationServiceManager = LocationServiceManager(context)
    }

    @Test
    fun onLocationChanged_doesNotThrowNpeWhenLastLocationIsNull() {
        val listener = mock<LocationUpdateListener>()
        locationServiceManager.addLocationListener(listener)

        val location = mock<Location>()
        whenever(location.latitude).thenReturn(10.0)
        whenever(location.longitude).thenReturn(20.0)
        whenever(location.accuracy).thenReturn(5.0f)

        locationServiceManager.onLocationChanged(location)

        verify(listener).onLocationChangedSignificantly(any())
    }
}
