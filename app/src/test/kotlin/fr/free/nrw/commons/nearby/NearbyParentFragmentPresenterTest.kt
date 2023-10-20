package fr.free.nrw.commons.nearby

import com.mapbox.mapboxsdk.annotations.Marker
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * The unit test class for NearbyParentFragmentPresenter
 */
class NearbyParentFragmentPresenterTest {
    @Mock
    internal lateinit var nearbyParentFragmentView: NearbyParentFragmentContract.View
    @Mock
    internal lateinit var bookmarkLocationsDao: BookmarkLocationsDao
    @Mock
    internal lateinit var latestLocation: LatLng
    @Mock
    internal lateinit var cameraTarget: LatLng
    @Mock
    internal lateinit var selectedLabels: List<Label>
    @Mock
    internal lateinit var marker: Marker
    @Mock
    internal lateinit var nearbyPlaces: NearbyPlaces

    private lateinit var nearbyPresenter: NearbyParentFragmentPresenter
    private lateinit var mapboxCameraTarget: com.mapbox.mapboxsdk.geometry.LatLng

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        nearbyPresenter = NearbyParentFragmentPresenter(bookmarkLocationsDao)
        nearbyPresenter.attachView(nearbyParentFragmentView)
    }

    /**
     * Tests nearby operations are initialized
     */
    @Test
    fun testInitializeNearbyMapOperations() {
        nearbyPresenter.initializeMapOperations()
        verify(nearbyParentFragmentView).enableFABRecenter()
        expectMapAndListUpdate()
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(nearbyParentFragmentView).disableFABRecenter();
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(latestLocation)
        verify(nearbyParentFragmentView).addSearchThisAreaButtonAction()
        verify(nearbyParentFragmentView).setCheckBoxAction()
    }

    /**
     * Test lockUnlockNearby method to lock nearby case
     */
    @Test
    fun testLockUnlockNearbyForLocked() {
        nearbyPresenter.lockUnlockNearby(true)
        verify(nearbyParentFragmentView).disableFABRecenter()
    }

    /**
     * Test lockUnlockNearby method to unlock nearby case
     */
    @Test
    fun testLockUnlockNearbyForUnlocked() {
        nearbyPresenter.lockUnlockNearby(false)
        verify(nearbyParentFragmentView).enableFABRecenter()
    }

    /**
     * Test updateMapAndList method returns with zero interactions when location is locked
     */
    @Test
    fun testUpdateMapAndListWhenLocationLocked() {
        nearbyPresenter.lockUnlockNearby(true)
        nearbyPresenter.updateMapAndList(null)
        verify(nearbyParentFragmentView).disableFABRecenter()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test updateMapAndList method returns with zero interactions when network connection
     * is not established
     */
    @Test
    fun testUpdateMapAndListWhenNoNetworkConnection() {
        nearbyPresenter.lockUnlockNearby(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(false)
        nearbyPresenter.updateMapAndList(null)
        verify(nearbyParentFragmentView).enableFABRecenter()
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test updateMapAndList method returns with zero interactions when last location is null
     */
    @Test
    fun testUpdateMapAndListWhenLastLocationIsNull() {
        nearbyPresenter.lockUnlockNearby(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(null)
        nearbyPresenter.updateMapAndList(null)
        verify(nearbyParentFragmentView).enableFABRecenter()
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verify(nearbyParentFragmentView).getLastLocation()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test updateMapAndList method updates parent fragment view with latest location of user
     * at significant location change
     */
    @Test
    fun testPlacesPopulatedForLatestLocationWhenLocationSignificantlyChanged() {
        expectMapAndListUpdate()
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        updateMapSignificantly()
    }

    /**
     * Test updateMapAndList method updates parent fragment view with latest location of user
     * at map is updated location change type
     */
    @Test
    fun testPlacesPopulatedForLatestLocationWhenLocationMapUpdated() {
        expectMapAndListUpdate()
        nearbyPresenter.updateMapAndList(LocationChangeType.MAP_UPDATED)
        updateMapSignificantly()
    }

    fun updateMapSignificantly() {
        verify(nearbyParentFragmentView).disableFABRecenter()
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(latestLocation)
    }

    /**
     * Test updateMapAndList method updates parent fragment view with camera target location
     * at search custom area mode
     */
    @Test
    fun testPlacesPopulatedForCameraTargetLocationWhenSearchCustomArea() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.getCameraTarget()).thenReturn(cameraTarget)
        nearbyPresenter.updateMapAndList(LocationChangeType.SEARCH_CUSTOM_AREA)
        verify(nearbyParentFragmentView).disableFABRecenter()
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(cameraTarget)
    }

    /**
     * Test testUpdateMapAndList tracks users location if current location marker is visible and
     * location is slightly changed
     */
    @Test
    fun testUserTrackedWhenCurrentLocationMarkerVisible() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.isCurrentLocationMarkerVisible()).thenReturn(true)
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).recenterMap(latestLocation)
    }

    /**
     * Test testUpdateMapAndList doesn't track users location if current location marker is
     * invisible and location is slightly changed
     */
    @Test
    fun testUserNotTrackedWhenCurrentLocationMarkerInvisible() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.isCurrentLocationMarkerVisible()).thenReturn(false)
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).enableFABRecenter()
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verify(nearbyParentFragmentView).getLastLocation()
        verify(nearbyParentFragmentView).isCurrentLocationMarkerVisible()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test search this area button became visible after user moved the camera target to far
     * away from current target. Distance between these two point is 111.19 km, so our camera target
     * is at outside of previously searched region if we set latestSearchRadius below 111.19. Thus,
     * setSearchThisAreaButtonVisibility(true) should be verified.
     */
    @Test
    fun testSearchThisAreaButtonVisibleWhenMoveToFarPosition() {
        NearbyController.latestSearchLocation = Mockito.spy(LatLng(2.0,1.0,0.0F))
        mapboxCameraTarget = Mockito.spy(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        // Distance between these two point is 111.19 km
        NearbyController.latestSearchRadius = 111.0*1000 // To meter
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        nearbyPresenter.onCameraMove(mapboxCameraTarget)
        verify(nearbyParentFragmentView).setSearchThisAreaButtonVisibility(true)
    }

    /**
     * Test search this area button became visible after user moved the camera target to far
     * away from current target. Distance between these two point is 111.19 km, so our camera target
     * is at inside of previously searched region if we set latestSearchRadius above 111.19. Thus,
     * setSearchThisAreaButtonVisibility(false) should be verified.
     */
    @Test
    fun testSearchThisAreaButtonInvisibleWhenMoveToClosePosition() {
        NearbyController.latestSearchLocation = Mockito.spy(LatLng(2.0,1.0,0.0F))
        mapboxCameraTarget = Mockito.spy(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        // Distance between these two point is 111.19 km
        NearbyController.latestSearchRadius = 112.0*1000 // To meter
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        nearbyPresenter.onCameraMove(mapboxCameraTarget)
        verify(nearbyParentFragmentView).setSearchThisAreaButtonVisibility(false)
    }

    /**
     * Multi selection should overwrite single selection of marker types. Ie. when user choose
     *"parks", then they multi select to display all or none, we overwrite previous "park" filter.
     *
     * We expect zero interaction from view when state is UNKNOWN
     */
    @Test
    fun testFilterByMarkerTypeMultiSelectUNKNOWN() {
        val state = CheckBoxTriStates.UNKNOWN
        nearbyPresenter.filterByMarkerType(selectedLabels,state,false,true)
        verifyNoInteractions(nearbyParentFragmentView)
    }

    /**
     * Multi selection should overwrite single selection of marker types. Ie. when user choose
     *"parks", then they multi select to display all or none, we overwrite previous "park" filter.
     *
     * We expect just filterOutAllMarkers and setRecyclerViewAdapterItemsGreyedOut is called when
     * the state is UNCHECKED
     */
    @Test
    fun testFilterByMarkerTypeMultiSelectUNCHECKED() {
        val state = CheckBoxTriStates.UNCHECKED
        nearbyPresenter.filterByMarkerType(selectedLabels,state,false,true)
        verify(nearbyParentFragmentView).filterOutAllMarkers()
        verify(nearbyParentFragmentView).setRecyclerViewAdapterItemsGreyedOut()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Multi selection should overwrite single selection of marker types. Ie. when user choose
     *"parks", then they multi select to display all or none, we overwrite previous "park" filter.
     *
     * We expect just displayAllMarkers and setRecyclerViewAdapterAllSelected is called when
     * the state is CHECKED
     */
    @Test
    fun testFilterByMarkerTypeMultiSelectCHECKED() {
        val state = CheckBoxTriStates.CHECKED
        nearbyPresenter.filterByMarkerType(selectedLabels, state, false,true)
        verify(nearbyParentFragmentView).filterMarkersByLabels(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyBoolean()
        );
        verify(nearbyParentFragmentView).setRecyclerViewAdapterAllSelected()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * We expect just filterMarkersByLabels is called when filterForAllNoneType is false
     */
    @Test
    fun testFilterByMarkerTypeSingleSelect() {
        nearbyPresenter.filterByMarkerType(selectedLabels, 0, true,false)
        verify(nearbyParentFragmentView).filterMarkersByLabels(
            any(),
            any(),
            any(),
            any(),
            any(),
            any()
        );
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test if bottom sheet gets hidden after search view gained focus
     */
    @Test
    fun testSearchViewFocusWhenBottomSheetExpanded() {
        whenever(nearbyParentFragmentView.isListBottomSheetExpanded()).thenReturn(true)
        nearbyPresenter.searchViewGainedFocus()
        verify(nearbyParentFragmentView).hideBottomSheet()
    }

    /**
     * Test if bottom details sheet gets hidden after search view gained focus
     */
    @Test
    fun testSearchViewFocusWhenDetailsBottomSheetVisible() {
        whenever(nearbyParentFragmentView.isListBottomSheetExpanded()).thenReturn(false)
        whenever(nearbyParentFragmentView.isDetailsBottomSheetVisible()).thenReturn(true)
        nearbyPresenter.searchViewGainedFocus()
        verify(nearbyParentFragmentView).hideBottomDetailsSheet()
    }

    /**
     * Test if the search is close to current location, when last location is null we expect it to
     * return true
     */
    @Test
    fun testSearchCloseToCurrentLocationNullLastLocation() {
        whenever(nearbyParentFragmentView.getLastFocusLocation()).thenReturn(null)
        val isClose = nearbyPresenter?.searchCloseToCurrentLocation()
        assertTrue(isClose!!)
    }

    /**
     * Test if the search is close to current location, when far
     */
    @Test
    fun testSearchCloseToCurrentLocationWhenFar() {
        whenever(nearbyParentFragmentView.getLastFocusLocation()).
            thenReturn(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        whenever(nearbyParentFragmentView.getCameraTarget()).
                thenReturn(LatLng(2.0,1.0,0.0F))
        //111.19 km real distance, return false if 148306.444306 >  currentLocationSearchRadius
        NearbyController.currentLocationSearchRadius = 148306.0
        val isClose = nearbyPresenter?.searchCloseToCurrentLocation()
        assertFalse(isClose!!)
    }

    /**
     * Test if the search is close to current location, when close
     */
    @Test
    fun testSearchCloseToCurrentLocationWhenClose() {
        whenever(nearbyParentFragmentView.getLastFocusLocation()).
            thenReturn(com.mapbox.mapboxsdk.geometry.LatLng(1.0,1.0,0.0))
        whenever(nearbyParentFragmentView.getCameraTarget()).
            thenReturn(LatLng(2.0,1.0,0.0F))
        //111.19 km real distance, return false if 148253.333 >  currentLocationSearchRadius
        NearbyController.currentLocationSearchRadius = 148307.0
        val isClose = nearbyPresenter?.searchCloseToCurrentLocation()
        assertTrue(isClose!!)
    }

    fun expectMapAndListUpdate() {
        nearbyPresenter.lockUnlockNearby(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
    }

    @Test
    fun testSetActionListeners() {
        nearbyPresenter.setActionListeners(any())
        verify(nearbyParentFragmentView).setFABPlusAction(any())
        verify(nearbyParentFragmentView).setFABRecenterAction(any())
    }

    @Test
    fun testBackButtonClickedWhenBottomSheetExpanded() {
        whenever(nearbyParentFragmentView.isListBottomSheetExpanded()).thenReturn(true)
        nearbyPresenter.backButtonClicked()
        verify(nearbyParentFragmentView).listOptionMenuItemClicked()
    }

    @Test
    fun testBackButtonClickedWhenDetailsBottomSheetVisible() {
        whenever(nearbyParentFragmentView.isListBottomSheetExpanded()).thenReturn(false)
        whenever(nearbyParentFragmentView.isDetailsBottomSheetVisible()).thenReturn(true)
        nearbyPresenter.backButtonClicked()
        verify(nearbyParentFragmentView).setBottomSheetDetailsSmaller()
    }

    @Test
    fun testBackButtonClickedWhenNoSheetVisible() {
        whenever(nearbyParentFragmentView.isListBottomSheetExpanded()).thenReturn(false)
        whenever(nearbyParentFragmentView.isDetailsBottomSheetVisible()).thenReturn(false)
        val hasNearbyHandledBackPress = nearbyPresenter.backButtonClicked()
        assertFalse(hasNearbyHandledBackPress)
    }

    @Test
    fun testBackButtonClickedWhenAdvancedFragmentIsVisible() {
        whenever(nearbyParentFragmentView.isAdvancedQueryFragmentVisible()).thenReturn(true)
        val hasNearbyHandledBackPress = nearbyPresenter.backButtonClicked()
        verify(nearbyParentFragmentView).showHideAdvancedQueryFragment(false)
        assertTrue(hasNearbyHandledBackPress)
    }

    @Test
    fun testMarkerUnselected() {
        nearbyPresenter.markerUnselected()
        verify(nearbyParentFragmentView).hideBottomSheet();
    }

    @Test
    fun testMarkerSelected() {
        nearbyPresenter.markerSelected(marker)
        verify(nearbyParentFragmentView).displayBottomSheetWithInfo(marker)
    }

    @Test
    fun testOnWikidataEditSuccessful() {
        nearbyPresenter.onWikidataEditSuccessful()
        expectMapAndListUpdate()
        nearbyPresenter.updateMapAndList(LocationChangeType.MAP_UPDATED)
        updateMapSignificantly()
    }

    @Test
    fun testOnLocationChangedSignificantly() {
        nearbyPresenter.onLocationChangedSignificantly(latestLocation)
        expectMapAndListUpdate()
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        updateMapSignificantly()
    }

    @Test
    fun testOnLocationChangedSlightly() {
        nearbyPresenter.onLocationChangedSlightly(latestLocation)
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.isCurrentLocationMarkerVisible()).thenReturn(true)
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).recenterMap(latestLocation)
    }

    @Test
    fun testOnLocationChangeTypeCustomQuery() {
        nearbyPresenter.setAdvancedQuery("Point(17.865 82.343)\"")
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(any(), any())
    }

    @Test
    fun testOnLocationChangeTypeCustomQueryInvalidQuery() {
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished).thenReturn(true)
        whenever(nearbyParentFragmentView.lastLocation).thenReturn(latestLocation)
        nearbyPresenter.setAdvancedQuery("")
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()
        nearbyPresenter.setAdvancedQuery("Point(")
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()
    }

    @Test
    fun testOnLocationChangeTypeCustomQueryUnParsableQuery() {
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished).thenReturn(true)
        whenever(nearbyParentFragmentView.lastLocation).thenReturn(latestLocation)
        nearbyPresenter.setAdvancedQuery("Point()\"")
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()

        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished).thenReturn(true)
        whenever(nearbyParentFragmentView.lastLocation).thenReturn(latestLocation)
        nearbyPresenter.setAdvancedQuery("Point(ab)\"")
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()

        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished).thenReturn(true)
        whenever(nearbyParentFragmentView.lastLocation).thenReturn(latestLocation)
        nearbyPresenter.setAdvancedQuery("Point(ab ab)\"")
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        expectMapAndListUpdate()
    }

    @Test
    fun testOnCameraMoveWhenSearchLocationNull() {
        NearbyController.latestSearchLocation = null
        nearbyPresenter.onCameraMove(Mockito.mock(com.mapbox.mapboxsdk.geometry.LatLng::class.java))
        verify(nearbyParentFragmentView).setProjectorLatLngBounds()
        verify(nearbyParentFragmentView).setSearchThisAreaButtonVisibility(false)
    }

    @Test
    fun testOnCameraMoveWhenNetworkConnectionNotEstablished() {
        NearbyController.latestSearchLocation = latestLocation
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(false)
        nearbyPresenter.onCameraMove(Mockito.mock(com.mapbox.mapboxsdk.geometry.LatLng::class.java))
        verify(nearbyParentFragmentView).setProjectorLatLngBounds()
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    @Test
    fun testOnCameraMoveWhenNetworkConnectionEstablished() {
        NearbyController.latestSearchLocation = latestLocation
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(false)
        nearbyPresenter.onCameraMove(Mockito.mock(com.mapbox.mapboxsdk.geometry.LatLng::class.java))
        verify(nearbyParentFragmentView).setProjectorLatLngBounds()
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    @Test
    fun testSetAdvancedQuery(){
        nearbyPresenter.setAdvancedQuery("test")
    }

    @Test
    fun testUpdateMapMarkers(){
        var nearbyPlacesInfo = NearbyController(nearbyPlaces).NearbyPlacesInfo()
        nearbyPlacesInfo.boundaryCoordinates= arrayOf()
        nearbyPlacesInfo.curLatLng=latestLocation
        nearbyPlacesInfo.searchLatLng=latestLocation
        nearbyPlacesInfo.placeList = null

        whenever(bookmarkLocationsDao.allBookmarksLocations).thenReturn(Collections.emptyList())
        nearbyPresenter.updateMapMarkers(nearbyPlacesInfo, marker, true)
        Mockito.verify(nearbyParentFragmentView).updateMapMarkers(any(), eq(marker))
        Mockito.verify(nearbyParentFragmentView).addCurrentLocationMarker(latestLocation)
        Mockito.verify(nearbyParentFragmentView).updateMapToTrackPosition(latestLocation)
        Mockito.verify(nearbyParentFragmentView).setProgressBarVisibility(false)
        Mockito.verify(nearbyParentFragmentView).centerMapToPosition(latestLocation)

    }
}
