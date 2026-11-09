package fr.free.nrw.commons.explore.map

import android.app.Activity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreMapLocationHelperTest {
    private lateinit var helper: ExploreMapLocationHelper
    private val locationManager: LocationServiceManager = mock()
    private val locationPermissionsHelper: LocationPermissionsHelper = mock()
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        helper = ExploreMapLocationHelper()
        helper.locationManager = locationManager
        helper.locationPermissionsHelper = locationPermissionsHelper
        helper.activityProvider = { activity }
    }

    /**
     * Verifies askForLocationPermission triggers callback.
     */
    @Test
    fun testAskForLocationPermission() {
        var callbackInvoked = false
        helper.onAskForLocationPermission = { callbackInvoked = true }
        helper.askForLocationPermission()
        assertTrue(callbackInvoked)
    }

    /**
     * Verifies getLastLocation retrieves from manager when not cached.
     */
    @Test
    fun testGetLastLocationFromManager() {
        val latLng = LatLng(10.0, 20.0, 0f)
        whenever(locationManager.getLastLocation()).thenReturn(latLng)
        val result = helper.getLastLocation()
        assertEquals(latLng, result)
    }

    /**
     * Verifies getLastLocation returns cached value if present.
     */
    @Test
    fun testGetLastLocationCached() {
        val cached = LatLng(30.0, 40.0, 0f)
        helper.lastKnownLocation = cached
        assertEquals(cached, helper.getLastLocation())
    }

    /**
     * Verifies significant location changes update location and invoke callback.
     */
    @Test
    fun testOnLocationChangedSignificantly() {
        val target = LatLng(1.0, 2.0, 0f)
        var receivedLatLng: LatLng? = null
        var receivedType: LocationChangeType? = null
        helper.onLocationUpdate = { lat, type ->
            receivedLatLng = lat
            receivedType = type
        }

        helper.onLocationChangedSignificantly(target)
        assertEquals(target, receivedLatLng)
        assertEquals(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED, receivedType)
    }

    /**
     * Verifies slight location changes update location and invoke callback.
     */
    @Test
    fun testOnLocationChangedSlightly() {
        val target = LatLng(3.0, 4.0, 0f)
        var receivedType: LocationChangeType? = null
        helper.onLocationUpdate = { _, type ->
            receivedType = type
        }

        helper.onLocationChangedSlightly(target)
        assertEquals(LocationChangeType.LOCATION_SLIGHTLY_CHANGED, receivedType)
    }

    /**
     * Verifies onLocationPermissionGranted triggers registration when location access is on.
     */
    @Test
    fun testOnLocationPermissionGranted() {
        whenever(locationPermissionsHelper.isLocationAccessToAppsTurnedOn()).thenReturn(true)
        var callbackInvoked = false
        helper.onPermissionGrantedCallback = { callbackInvoked = true }

        helper.onLocationPermissionGranted()
        verify(locationManager).registerLocationManager()
        assertTrue(callbackInvoked)
    }

    /**
     * Verifies requestLocationIfNeeded does nothing if fragment is not visible.
     */
    @Test
    fun testRequestLocationIfNeededNotVisible() {
        helper.fragmentVisibilityProvider = { false }
        var mapReadyActionsInvoked = false
        helper.onMapReadyActions = { mapReadyActionsInvoked = true }

        helper.requestLocationIfNeeded()
        assertNull(helper.lastKnownLocation)
    }

    /**
     * Verifies onLocationPermissionDenied does not throw exception.
     */
    @Test
    fun testOnLocationPermissionDenied() {
        helper.onLocationPermissionDenied("Denied")
        assertNotNull(helper)
    }

    /**
     * Verifies checkAndPerformResumeMapReadyActions invokes callback and resets flag when scheduled.
     */
    @Test
    fun testCheckAndPerformResumeMapReadyActionsWhenScheduled() {
        var callbackInvoked = false
        helper.onMapReadyActions = { callbackInvoked = true }
        helper.shouldPerformMapReadyActionsOnResume = true

        helper.checkAndPerformResumeMapReadyActions()

        assertTrue(callbackInvoked)
        assertFalse(helper.shouldPerformMapReadyActionsOnResume)
    }

    /**
     * Verifies checkAndPerformResumeMapReadyActions does nothing when not scheduled.
     */
    @Test
    fun testCheckAndPerformResumeMapReadyActionsWhenNotScheduled() {
        var callbackInvoked = false
        helper.onMapReadyActions = { callbackInvoked = true }
        helper.shouldPerformMapReadyActionsOnResume = false

        helper.checkAndPerformResumeMapReadyActions()

        assertFalse(callbackInvoked)
    }
}
