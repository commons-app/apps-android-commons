package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Before
import org.junit.Test
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
    private lateinit var nearbyPresenter: NearbyParentFragmentPresenter
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
}