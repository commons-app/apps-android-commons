package fr.free.nrw.commons.explore.map

import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao

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
        testLockUnlockMap()
        testUpdateMap()
        verify(exploreMapView).addSearchThisAreaButtonAction()
    }

    @Test
    fun testLockUnlockMap() {

    }

    @Test
    fun testUpdateMap() {

    }
}