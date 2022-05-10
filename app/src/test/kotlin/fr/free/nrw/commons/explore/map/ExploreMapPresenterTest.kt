package fr.free.nrw.commons.explore.map

import com.mapbox.mapboxsdk.annotations.Marker
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.PlacesInfo
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.NearbyBaseMarker
import fr.free.nrw.commons.nearby.NearbyController

import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.NullPointerException

/**
 * The unit test class for ExploreMapPresenter
 */
class ExploreMapPresenterTest {
    @Mock
    internal lateinit var exploreMapView: ExploreMapContract.View
    @Mock
    internal lateinit var bookmarkLocationsDao: BookmarkLocationsDao
    @Mock
    internal lateinit var latestLocation: LatLng
    @Mock
    internal lateinit var explorePlacesInfo: PlacesInfo

    private lateinit var exploreMapPresenter: ExploreMapPresenter
    private lateinit var mapboxCameraTarget: com.mapbox.mapboxsdk.geometry.LatLng


    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        exploreMapPresenter = ExploreMapPresenter(bookmarkLocationsDao)
        exploreMapPresenter.attachView(exploreMapView)
    }

    @Test
    fun initializeMapOperations() {
        exploreMapPresenter.initializeMapOperations()
        verify(exploreMapView).addSearchThisAreaButtonAction()
        expectMapAndListUpdate()
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(exploreMapView).getLastLocation()
        verify(exploreMapView).disableFABRecenter()
        verify(exploreMapView).setProgressBarVisibility(true)
        verify(exploreMapView).populatePlaces(latestLocation, latestLocation)
    }

    /**
     * Test lockUnlockNearby method to lock nearby case
     */
    @Test
    fun testLockUnlockNearbyForLocked() {
        exploreMapPresenter.lockUnlockNearby(true)
        verify(exploreMapView).disableFABRecenter()
    }

    /**
     * Test lockUnlockNearby method to unlock nearby case
     */
    @Test
    fun testLockUnlockNearbyForUnlocked() {
        exploreMapPresenter.lockUnlockNearby(false)
        verify(exploreMapView).enableFABRecenter()
    }

    @Test
    fun testUpdateMapAndListWhenLocationLocked() {
        exploreMapPresenter.lockUnlockNearby(true)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(exploreMapView).disableFABRecenter()
        verifyZeroInteractions(exploreMapView)
    }

    @Test
    fun testUpdateMapAndListWhenLocationUnlocked() {
        exploreMapPresenter.lockUnlockNearby(false)
        whenever(exploreMapView.isNetworkConnectionEstablished()).thenReturn(false)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(exploreMapView).enableFABRecenter()
        verify(exploreMapView).isNetworkConnectionEstablished()
        verifyZeroInteractions(exploreMapView)
    }

    @Test
    fun testUpdateMapAndListWhenNoNetworkConnection() {
        exploreMapPresenter.lockUnlockNearby(false)
        whenever(exploreMapView.isNetworkConnectionEstablished()).thenReturn(false)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(exploreMapView).enableFABRecenter()
        verify(exploreMapView).isNetworkConnectionEstablished()
        verifyNoMoreInteractions(exploreMapView)
    }

    @Test
    fun testUpdateMapAndListWhenLastLocationIsNull() {
        exploreMapPresenter.lockUnlockNearby(false)
        whenever(exploreMapView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(exploreMapView.getLastLocation()).thenReturn(null)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(exploreMapView).enableFABRecenter()
        verify(exploreMapView).isNetworkConnectionEstablished()
        verify(exploreMapView).getLastLocation()
        verifyNoMoreInteractions(exploreMapView)
    }

    @Test
    fun testUpdateMapWhenLocationSignificantlyChanged() {
        expectMapAndListUpdate()
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        updateMapSignificantly()
    }

    @Test
    fun testUpdateMapWhenSearchCustomArea() {
        expectMapAndListUpdate()
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA)
        updateMapSearchCustomArea()
    }

    @Test
    fun testUserTrackedWhenCurrentLocationMarkerVisible() {
        expectMapAndListUpdate()
        whenever(exploreMapView.isCurrentLocationMarkerVisible()).thenReturn(true)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(exploreMapView).recenterMap(latestLocation)
    }

    @Test
    fun testUserNotTrackedWhenCurrentLocationMarkerInvisible() {
        expectMapAndListUpdate()
        whenever(exploreMapView.isCurrentLocationMarkerVisible()).thenReturn(false)
        exploreMapPresenter.updateMap(LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(exploreMapView).enableFABRecenter()
        verify(exploreMapView).isNetworkConnectionEstablished()
        verify(exploreMapView).getLastLocation()
        verify(exploreMapView).isCurrentLocationMarkerVisible()
        verifyNoMoreInteractions(exploreMapView)
    }

    @Test(expected = NullPointerException::class)
    fun testUpdateMapMarkers() {
        explorePlacesInfo.curLatLng = latestLocation
        explorePlacesInfo.boundaryCoordinates = arrayOf<LatLng>(
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f))
        exploreMapPresenter.updateMapMarkers(explorePlacesInfo, null, true)
        verify(exploreMapView).setMapBoundaries(any())
    }

    @Test(expected = NullPointerException::class)
    fun testOnNearbyBaseMarkerThumbsReadyShouldTrack() {
        explorePlacesInfo.curLatLng = latestLocation
        explorePlacesInfo.boundaryCoordinates = arrayOf<LatLng>(
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f))

        exploreMapPresenter.onNearbyBaseMarkerThumbsReady(mutableListOf(NearbyBaseMarker()), explorePlacesInfo, null, true)
        verify(exploreMapView).addNearbyMarkersToMapBoxMap(mutableListOf(NearbyBaseMarker()), null)
        verify(exploreMapView).addCurrentLocationMarker(latestLocation)
        verify(exploreMapView).updateMapToTrackPosition(latestLocation)
        verify(exploreMapView).setProgressBarVisibility(false)
    }

    @Test(expected = NullPointerException::class)
    fun testOnNearbyBaseMarkerThumbsReadyShouldNotTrack() {
        explorePlacesInfo.curLatLng = latestLocation
        explorePlacesInfo.boundaryCoordinates = arrayOf<LatLng>(
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f),
            LatLng(1.0,1.0,0.0f))

        exploreMapPresenter.onNearbyBaseMarkerThumbsReady(mutableListOf(NearbyBaseMarker()), explorePlacesInfo, null, true)
        verify(exploreMapView).addNearbyMarkersToMapBoxMap(mutableListOf(NearbyBaseMarker()), null)
        verify(exploreMapView).addCurrentLocationMarker(latestLocation)
        verify(exploreMapView).setProgressBarVisibility(false)
    }

    @Test
    fun testSearchCloseToCurrentLocationLastFocusNull() {
        whenever(exploreMapView.lastFocusLocation).thenReturn(null)
        val result = exploreMapPresenter.searchCloseToCurrentLocation()
        Assertions.assertEquals(result, true)
    }

    @Test(expected = NullPointerException::class)
    fun testSearchCloseToCurrentLocationWhenClose() {
        whenever(exploreMapView.lastFocusLocation).thenReturn(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        whenever(exploreMapView.cameraTarget).thenReturn(LatLng(1.0,1.0,0.0f))
        val result = exploreMapPresenter.searchCloseToCurrentLocation()
        Assertions.assertEquals(result, true)
    }

    @Test(expected = NullPointerException::class)
    fun testSearchCloseToCurrentLocationWhenFar() {
        whenever(exploreMapView.lastFocusLocation).thenReturn(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        whenever(exploreMapView.cameraTarget).thenReturn(LatLng(100.0,1.0,0.0f))
        val result = exploreMapPresenter.searchCloseToCurrentLocation()
        Assertions.assertEquals(result, false)
    }

    @Test
    fun testMarkerUnselected() {
        exploreMapPresenter.markerUnselected()
        verify(exploreMapView).hideBottomDetailsSheet()
    }

    @Test
    fun testMarkerSelected() {
        exploreMapPresenter.markerSelected(Marker(NearbyBaseMarker()))
        verify(exploreMapView).displayBottomSheetWithInfo(Marker(NearbyBaseMarker()))
    }

    fun expectMapAndListUpdate() {
        exploreMapPresenter.lockUnlockNearby(false)
        whenever(exploreMapView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(exploreMapView.getLastLocation()).thenReturn(latestLocation)
    }

    fun updateMapSignificantly() {
        verify(exploreMapView).disableFABRecenter()
        verify(exploreMapView).setProgressBarVisibility(true)
        verify(exploreMapView).populatePlaces(latestLocation,latestLocation)
    }

    fun updateMapSearchCustomArea() {
        verify(exploreMapView).disableFABRecenter()
        verify(exploreMapView).setProgressBarVisibility(true)
        verify(exploreMapView).getCameraTarget()
        verify(exploreMapView).populatePlaces(latestLocation,exploreMapView.cameraTarget)
    }

}