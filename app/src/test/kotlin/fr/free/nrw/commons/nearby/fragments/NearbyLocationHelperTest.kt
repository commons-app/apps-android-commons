package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyLocationHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyLocationHelperTest {
    private lateinit var helper: NearbyLocationHelper
    private lateinit var activity: Activity
    private lateinit var locationManager: LocationServiceManager
    private lateinit var locationPermissionsHelper: LocationPermissionsHelper
    private lateinit var applicationKvStore: JsonKvStore

    private var mapReadyCalled = false
    private var lastReportedLocationChangeType: LocationChangeType? = null
    private var lastOverlayGeoPoint: GeoPoint? = null
    private var cameraMoveRequestedGeoPoint: GeoPoint? = null
    private var cameraCenterRequestedGeoPoint: GeoPoint? = null
    private var progressBarVisibleState: Boolean? = null
    private var locationOffDialogShown = false
    private var askForLocationPermissionCalled = false

    private val actions =
        object : NearbyLocationHelper.NearbyLocationActions {
            override fun onMapReady() {
                mapReadyCalled = true
            }

            override fun onUpdateMapAndList(locationChangeType: LocationChangeType) {
                lastReportedLocationChangeType = locationChangeType
            }

            override fun onUserLocationOverlaysUpdated(
                targetP: GeoPoint,
                invalidate: Boolean,
            ) {
                lastOverlayGeoPoint = targetP
            }

            override fun onCameraMoveRequested(targetP: GeoPoint) {
                cameraMoveRequestedGeoPoint = targetP
            }

            override fun onCameraCenterRequested(targetP: GeoPoint) {
                cameraCenterRequestedGeoPoint = targetP
            }

            override fun isCameFromExploreMap(): Boolean = false

            override fun isDepictsPickerMode(): Boolean = false

            override fun setProgressBarVisibility(isVisible: Boolean) {
                progressBarVisibleState = isVisible
            }

            override fun showLocationOffDialog() {
                locationOffDialogShown = true
            }

            override fun askForLocationPermission() {
                askForLocationPermissionCalled = true
            }
        }

    /**
     * Prepares test fixtures and initializes the helper.
     */
    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        locationManager = mock()
        locationPermissionsHelper = mock()
        applicationKvStore = mock()
        helper = NearbyLocationHelper()

        helper.init(
            activityProvider = { activity },
            locationManagerProvider = { locationManager },
            locationPermissionsHelperProvider = { locationPermissionsHelper },
            applicationKvStoreProvider = { applicationKvStore },
            actions = actions,
        )
    }

    @Test
    fun testAskForLocationPermissionTriggersAction() {
        helper.askForLocationPermission()
        assertTrue(askForLocationPermissionCalled)
    }

    @Test
    fun testRecenterMapWhenPermissionGranted() {
        whenever(locationPermissionsHelper.checkLocationPermission(activity)).thenReturn(true)
        helper.recenterMap(LatLng(10.0, 20.0, 1f))
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testRecenterMapWhenPermissionDenied() {
        whenever(locationPermissionsHelper.checkLocationPermission(activity)).thenReturn(false)
        helper.recenterMap(LatLng(10.0, 20.0, 1f))
        assertTrue(askForLocationPermissionCalled)
    }

    @Test
    fun testRecenterMapWhenPreviouslyDeniedAndNowGranted() {
        helper.isPermissionDenied = true
        whenever(locationPermissionsHelper.checkLocationPermission(activity)).thenReturn(true)
        helper.recenterMap(LatLng(10.0, 20.0, 1f))
        assertFalse(helper.isPermissionDenied)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testRecenterMapWithNullCoordinateSetsRecenterFlag() {
        whenever(locationPermissionsHelper.checkLocationPermission(activity)).thenReturn(false)
        helper.recenterMap(null)
        assertTrue(helper.recenterToUserLocation)
    }

    @Test
    fun testLocationPermissionGrantedWithLastLocation() {
        val latLng = LatLng(12.34, 56.78, 1f)
        whenever(locationManager.getLastLocation()).thenReturn(latLng)

        helper.locationPermissionGranted()

        assertNotNull(lastOverlayGeoPoint)
        assertEquals(12.34, lastOverlayGeoPoint!!.latitude, 0.001)
        assertEquals(56.78, lastOverlayGeoPoint!!.longitude, 0.001)
        assertNotNull(cameraCenterRequestedGeoPoint)
        assertEquals(12.34, cameraCenterRequestedGeoPoint!!.latitude, 0.001)
        assertEquals(56.78, cameraCenterRequestedGeoPoint!!.longitude, 0.001)
        assertNotNull(cameraMoveRequestedGeoPoint)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testLocationPermissionGrantedWithoutLastLocationAndGpsEnabled() {
        whenever(locationManager.getLastLocation()).thenReturn(null)
        whenever(locationManager.isGPSProviderEnabled()).thenReturn(true)

        helper.locationPermissionGranted()

        verify(locationManager).requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
        verify(locationManager).requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
        assertEquals(true, progressBarVisibleState)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testLocationPermissionGrantedWithoutLastLocationAndNoProviders() {
        whenever(locationManager.getLastLocation()).thenReturn(null)
        whenever(locationManager.isGPSProviderEnabled()).thenReturn(false)
        whenever(locationManager.isNetworkProviderEnabled()).thenReturn(false)

        helper.locationPermissionGranted()

        assertTrue(locationOffDialogShown)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testStartMapWithoutPermissionWithStoredLocation() {
        whenever(applicationKvStore.getString("LastLocation")).thenReturn("15.5,25.5")

        helper.startMapWithoutPermission()

        assertNotNull(helper.lastKnownLocation)
        assertEquals(15.5, helper.lastKnownLocation!!.latitude, 0.001)
        assertEquals(25.5, helper.lastKnownLocation!!.longitude, 0.001)
        assertNotNull(cameraMoveRequestedGeoPoint)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testStartMapWithoutPermissionWithoutStoredLocation() {
        whenever(applicationKvStore.getString("LastLocation")).thenReturn(null)

        helper.startMapWithoutPermission()

        assertNotNull(helper.lastKnownLocation)
        assertNotNull(cameraMoveRequestedGeoPoint)
        assertTrue(mapReadyCalled)
    }

    @Test
    fun testGetLastLocationReturnsCachedOrFallback() {
        val fallback = helper.getLastLocation()
        assertNotNull(fallback)

        val cached = LatLng(30.0, 40.0, 1f)
        helper.lastKnownLocation = cached
        assertEquals(cached, helper.getLastLocation())
    }

    @Test
    fun testGetMapCenterWithMapCenterGeoPoint() {
        helper.mapCenter = GeoPoint(11.11, 22.22)
        val center = helper.getMapCenter()
        assertEquals(11.11, center.latitude, 0.001)
        assertEquals(22.22, center.longitude, 0.001)
    }

    @Test
    fun testGetMapCenterWhenMapCenterNullWithStoredLocation() {
        whenever(applicationKvStore.getString("LastLocation")).thenReturn("33.44,55.66")
        helper.mapCenter = null
        val center = helper.getMapCenter()
        assertEquals(33.44, center.latitude, 0.001)
        assertEquals(55.66, center.longitude, 0.001)
    }

    @Test
    fun testGetMapCenterWhenMapCenterNullWithoutStoredLocation() {
        whenever(applicationKvStore.getString("LastLocation")).thenReturn(null)
        helper.mapCenter = null
        helper.lastKnownLocation = null
        val center = helper.getMapCenter()
        assertEquals(51.50550, center.latitude, 0.001)
        assertEquals(-0.07520, center.longitude, 0.001)
    }

    @Test
    fun testRegisterUnregisterLocationListenerAdd() {
        helper.registerUnregisterLocationListener(false)
        verify(locationManager).addLocationListener(any())
        verify(locationManager).registerLocationManager()
    }

    @Test
    fun testRegisterUnregisterLocationListenerRemove() {
        helper.registerUnregisterLocationListener(true)
        verify(locationManager).removeLocationListener(any())
        verify(locationManager).unregisterLocationManager()
    }

    @Test
    fun testLocationUpdateListenerSignificantly() {
        val latLng = LatLng(15.0, 20.0, 1f)
        helper.locationUpdateListener.onLocationChangedSignificantly(latLng)
        assertEquals(latLng, helper.lastKnownLocation)
        assertEquals(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED, lastReportedLocationChangeType)
        assertNotNull(lastOverlayGeoPoint)
    }

    @Test
    fun testLocationUpdateListenerSlightly() {
        val latLng = LatLng(15.0, 20.0, 1f)
        helper.locationUpdateListener.onLocationChangedSlightly(latLng)
        assertEquals(latLng, helper.lastKnownLocation)
        assertEquals(LocationChangeType.LOCATION_SLIGHTLY_CHANGED, lastReportedLocationChangeType)
    }

    @Test
    fun testLocationUpdateListenerMedium() {
        val latLng = LatLng(15.0, 20.0, 1f)
        helper.locationUpdateListener.onLocationChangedMedium(latLng)
        assertEquals(latLng, helper.lastKnownLocation)
        assertEquals(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED, lastReportedLocationChangeType)
    }

    @Test
    fun testOpenLocationSettingsRunsWithoutError() {
        helper.openLocationSettings()
        // verify execution without crash
        assertTrue(true)
    }

    @Test
    fun testLocationPermissionCallbackMethods() {
        helper.locationPermissionCallback.onLocationPermissionDenied("Denied")
        helper.locationPermissionCallback.onLocationPermissionGranted()
        assertTrue(true)
    }
}
