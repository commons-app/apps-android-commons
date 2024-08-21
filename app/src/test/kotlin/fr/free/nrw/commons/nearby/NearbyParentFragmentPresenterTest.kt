package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyBoolean
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
    internal lateinit var nearbyPlaces: NearbyPlaces

    private lateinit var nearbyPresenter: NearbyParentFragmentPresenter

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
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        verify(nearbyParentFragmentView).disableFABRecenter();
        verify(nearbyParentFragmentView).`setProgressBarVisibility`(true)
        assertTrue(null == nearbyParentFragmentView.mapCenter)
        verify(nearbyParentFragmentView).populatePlaces(null)
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
    }

    /**
     * Test updateMapAndList method returns with zero interactions when last location is null
     */
    @Test
    fun testUpdateMapAndListWhenLastLocationIsNull() {
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(null)
        whenever(nearbyParentFragmentView.getLastMapFocus()).thenReturn(null)
        whenever(nearbyParentFragmentView.getMapCenter()).thenReturn(null)
        nearbyPresenter.updateMapAndList(null)
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished
        verify(nearbyParentFragmentView).lastMapFocus
        verify(nearbyParentFragmentView).mapCenter
    }

    /**
     * Test updateMapAndList method updates parent fragment view with latest location of user
     * at significant location change
     */
     @Test
    fun testPlacesPopulatedForLatestLocationWhenLocationSignificantlyChanged() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(LatLng(2.0, 1.0, 0.0F));
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
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(LatLng(2.0, 1.0, 0.0F));
        nearbyPresenter.updateMapAndList(LocationChangeType.MAP_UPDATED)
        updateMapSignificantly()
    }

    fun updateMapSignificantly() {
        verify(nearbyParentFragmentView).disableFABRecenter()
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(any<LatLng>())
    }

    /**
     * Test updateMapAndList method updates parent fragment view with camera target location
     * at search custom area mode
     */
    @Test
    fun testPlacesPopulatedForCameraTargetLocationWhenSearchCustomArea() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.getCameraTarget()).thenReturn(LatLng(2.0, 1.0, 0.0F))
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(LatLng(2.0, 1.0, 0.0F));
        whenever(nearbyParentFragmentView.mapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        nearbyPresenter.updateMapAndList(LocationChangeType.SEARCH_CUSTOM_AREA)
        verify(nearbyParentFragmentView).disableFABRecenter()
        verify(nearbyParentFragmentView).setProgressBarVisibility(true)
        verify(nearbyParentFragmentView).populatePlaces(nearbyParentFragmentView.mapFocus)
    }

    /**
     * Test testUpdateMapAndList tracks users location if current location marker is visible and
     * location is slightly changed
     */
    @Test
    fun testUserTrackedWhenCurrentLocationMarkerVisible() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(null);
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).getLastMapFocus()
        verify(nearbyParentFragmentView).recenterMap(nearbyParentFragmentView.lastMapFocus)
    }

    /**
     * Test testUpdateMapAndList doesn't track users location if current location marker is
     * invisible and location is slightly changed
     */
    @Test
    fun testUserNotTrackedWhenCurrentLocationMarkerInvisible() {
        expectMapAndListUpdate()
        verify(nearbyParentFragmentView).enableFABRecenter()
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(null);
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).isNetworkConnectionEstablished()
        verify(nearbyParentFragmentView).getLastMapFocus()
        verify(nearbyParentFragmentView).getMapCenter()
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
        nearbyPresenter.filterByMarkerType(selectedLabels, state, false, true)
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
        nearbyPresenter.filterByMarkerType(selectedLabels, state, false, true)
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
        nearbyPresenter.filterByMarkerType(selectedLabels, state, false, true)
        verify(nearbyParentFragmentView).filterMarkersByLabels(
            ArgumentMatchers.anyList(),
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
        nearbyPresenter.filterByMarkerType(selectedLabels, 0, true, false)
        verify(nearbyParentFragmentView).filterMarkersByLabels(
            any(),
            anyBoolean(),
            anyBoolean()
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
     * Test if the search is close to current location, when far
     */
    @Test
    fun testSearchCloseToCurrentLocationWhenFar() {
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F));
        whenever(nearbyParentFragmentView.mapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        //111.19 km real distance, return false if 148306.444306 >  currentLocationSearchRadius
        NearbyController.currentLocationSearchRadius = 148306.0
        val isClose = nearbyPresenter?.searchCloseToCurrentLocation()
        assertFalse(isClose!!.equals(false))
    }

    /**
     * Test if the search is close to current location, when close
     */
    @Test
    fun testSearchCloseToCurrentLocationWhenClose() {
        whenever(nearbyParentFragmentView.getCameraTarget()).thenReturn(LatLng(2.0, 1.0, 0.0F))
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
    fun testOnWikidataEditSuccessful() {
        nearbyPresenter.onWikidataEditSuccessful()
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(LatLng(2.0, 1.0, 0.0F));
        nearbyPresenter.updateMapAndList(LocationChangeType.MAP_UPDATED)
        updateMapSignificantly()
    }

    @Test
    fun testOnLocationChangedSignificantly() {
        expectMapAndListUpdate()
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(LatLng(2.0, 1.0, 0.0F));
       latestLocation=LatLng(2.0, 1.0, 0.0F)
        nearbyPresenter.onLocationChangedSignificantly(latestLocation)
        updateMapSignificantly()
    }

    @Test
    fun testOnLocationChangedSlightly() {
        nearbyPresenter.onLocationChangedSlightly(latestLocation)
        expectMapAndListUpdate()
        verify(nearbyParentFragmentView).enableFABRecenter()
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(null)
        nearbyPresenter.updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        verify(nearbyParentFragmentView).getLastMapFocus()
        verify(nearbyParentFragmentView).recenterMap(nearbyParentFragmentView.lastMapFocus)
    }

    @Test
    fun testOnLocationChangeTypeCustomQuery() {
        nearbyPresenter.setAdvancedQuery("Point(17.865 82.343)\"")
        whenever(nearbyParentFragmentView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(nearbyParentFragmentView.getLastLocation()).thenReturn(latestLocation)
        whenever(nearbyParentFragmentView.lastMapFocus).thenReturn(LatLng(2.0, 1.0, 0.0F))
        whenever(nearbyParentFragmentView.mapCenter).thenReturn(null)
        nearbyPresenter.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
        verify(nearbyParentFragmentView).getLastMapFocus()
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
    fun testSetAdvancedQuery() {
        nearbyPresenter.setAdvancedQuery("test")
    }

    @Test
    fun testUpdateMapMarkers() {
        whenever(latestLocation.latitude).thenReturn(2.0)
        whenever(latestLocation.longitude).thenReturn(1.0)
        whenever(latestLocation.accuracy).thenReturn(0.0F)
        var nearbyPlacesInfo = NearbyController(nearbyPlaces).NearbyPlacesInfo()
        nearbyPlacesInfo.boundaryCoordinates = arrayOf()
        nearbyPlacesInfo.currentLatLng = latestLocation
        nearbyPlacesInfo.searchLatLng = latestLocation
        nearbyPlacesInfo.placeList = null

        whenever(bookmarkLocationsDao.allBookmarksLocations).thenReturn(Collections.emptyList())
        nearbyPresenter.updateMapMarkers(nearbyPlacesInfo.placeList, latestLocation, true)
        Mockito.verify(nearbyParentFragmentView).updateMapMarkers(any())
        Mockito.verify(nearbyParentFragmentView).setProgressBarVisibility(false)
        Mockito.verify(nearbyParentFragmentView).updateListFragment(nearbyPlacesInfo.placeList)

    }
}
