package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations


/**
 * The unit test class for NearbyParentFragmentPresenter
 */
class NearbyParentFragmentPresenterTest {
    @Mock
    internal lateinit var nearbyParentFragmentView: NearbyParentFragmentContract.View
    @Mock
    internal lateinit var bookmarkLocationsDao: BookmarkLocationsDao
    private lateinit var nearbyPresenter: NearbyParentFragmentPresenter
    /**
     * initial setup
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        nearbyPresenter = NearbyParentFragmentPresenter(bookmarkLocationsDao)
        nearbyPresenter.attachView(nearbyParentFragmentView)
    }

    /**
     * Tests if initializeNearbyMapOperations called
     */
    @Test
    fun testInitializeNearbyMapOperations() {
        nearbyPresenter.initializeMapOperations()
        verify(nearbyParentFragmentView).addSearchThisAreaButtonAction()
        verify(nearbyParentFragmentView).setCheckBoxAction()
    }
}