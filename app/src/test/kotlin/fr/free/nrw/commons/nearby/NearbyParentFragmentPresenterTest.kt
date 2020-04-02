package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations


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

    private lateinit var nearbyPresenter: NearbyParentFragmentPresenter
    private lateinit var latestLocationSpy: LatLng
    private lateinit var mapboxCameraTarget: com.mapbox.mapboxsdk.geometry.LatLng

    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        nearbyPresenter = Mockito.spy(NearbyParentFragmentPresenter(bookmarkLocationsDao))
        nearbyPresenter.attachView(nearbyParentFragmentView)
    }

    /**
     * Tests nearby operations are initialized
     */
    @Test
    fun testInitializeNearbyMapOperations() {
        nearbyPresenter.initializeMapOperations()
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
        nearbyPresenter.setNearbyLocked(true)
        nearbyPresenter.updateMapAndList(null)
        verifyZeroInteractions(nearbyParentFragmentView)
    }

    /**
     * Test updateMapAndList method returns with zero interactions when network connection
     * is not established
     */
    @Test
    fun testUpdateMapAndListWhenNoNetworkConnection() {
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(false)
        nearbyPresenter.updateMapAndList(null)
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * Test updateMapAndList method returns with zero interactions when last location is null
     */
    @Test
    fun testUpdateMapAndListWhenLastLocationIsNull() {
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(null)
        nearbyPresenter.updateMapAndList(null)
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
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        nearbyPresenter.updateMapAndList(LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(nearbyParentFragmentView).populatePlaces(latestLocation)
    }

    /**
     * Test updateMapAndList method updates parent fragment view with camera target location
     * at search custom area mode
     */
    @Test
    fun testPlacesPopulatedForCameraTargetLocationWhenSearchCustomArea() {
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        whenever(nearbyParentFragmentView.getCameraTarget()).thenReturn(cameraTarget)
        nearbyPresenter.updateMapAndList(LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA)
        verify(nearbyParentFragmentView).populatePlaces(cameraTarget)
    }

    /**
     * Test testUpdateMapAndList tracks users location if current location marker is visible and
     * location is slightly changed
     */
    @Test
    fun testUserTrackedWhenCurrentLocationMarkerVisible() {
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        whenever(nearbyParentFragmentView.isCurrentLocationMarkerVisible()).thenReturn(true)
        nearbyPresenter.updateMapAndList(LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).recenterMap(latestLocation)
    }

    /**
     * Test testUpdateMapAndList doesn't track users location if current location marker is
     * invisible and location is slightly changed
     */
    @Test
    fun testUserNotTrackedWhenCurrentLocationMarkerInvisible() {
        nearbyPresenter.setNearbyLocked(false)
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        whenever(nearbyParentFragmentView.isCurrentLocationMarkerVisible()).thenReturn(false)
        nearbyPresenter.updateMapAndList(LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
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
        verifyZeroInteractions(nearbyParentFragmentView)
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
        verify(nearbyParentFragmentView).displayAllMarkers()
        verify(nearbyParentFragmentView).setRecyclerViewAdapterAllSelected()
        verifyNoMoreInteractions(nearbyParentFragmentView)
    }

    /**
     * We expect just filterMarkersByLabels is called when filterForAllNoneType is false
     */
    @Test
    fun testFilterByMarkerTypeSingleSelect() {
        nearbyPresenter.filterByMarkerType(selectedLabels, 0, true,false)
        verify(nearbyParentFragmentView).filterMarkersByLabels(any(), any(), any(), any(), any());
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
}