package fr.free.nrw.commons.explore.map

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

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

    private lateinit var exploreMapPresenter: ExploreMapPresenter


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

    fun expectMapAndListUpdate() {
        exploreMapPresenter.lockUnlockNearby(false)
        whenever(exploreMapView.isNetworkConnectionEstablished()).thenReturn(true)
        whenever(exploreMapView.getLastLocation()).thenReturn(latestLocation)
    }
}