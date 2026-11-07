package fr.free.nrw.commons.nearby.presenter

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations

/**
 * Unit test suite for [NearbyPlaceDetailsLoader].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyPlaceDetailsLoaderTest {
    @Mock
    internal lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Mock
    internal lateinit var placesRepository: PlacesRepository

    @Mock
    internal lateinit var nearbyController: NearbyController

    @Mock
    internal lateinit var view: NearbyParentFragmentContract.View

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var loader: NearbyPlaceDetailsLoader

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        loader =
            NearbyPlaceDetailsLoader(
                bookmarkLocationDao,
                placesRepository,
                nearbyController,
            ) { view }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHandlePinClickedAddsPlace() {
        val place =
            Place().apply {
                name = "Test Place"
                location = LatLng(0.0, 0.0, 0f)
                entityID = "Q123"
            }
        loader.handlePinClicked(place)
        assertTrue(loader.clickedPlaces.contains(place))
    }

    @Test
    fun testCreateBatchesSplitsCorrectly() =
        runBlocking {
            val numbers = mutableListOf(1, 2, 3, 4, 5)
            val channel = loader.createBatches(numbers, 2)
            val batches = mutableListOf<List<Int>>()
            for (batch in channel) {
                batches.add(batch)
            }
            assertEquals(3, batches.size)
            assertEquals(listOf(1, 2), batches[0])
            assertEquals(listOf(3, 4), batches[1])
            assertEquals(listOf(5), batches[2])
        }

    @Test
    fun testLoadCachedPlacesWhenFoundInCache() =
        runBlocking {
            val cachedPlace =
                Place().apply {
                    entityID = "Q1"
                    name = "Cached Name"
                    label = Label.PARK
                    longDescription = "Cached Desc"
                    location = LatLng(1.0, 1.0, 0f)
                }
            whenever(placesRepository.fetchPlace("Q1")).thenReturn(cachedPlace)
            whenever(bookmarkLocationDao.findBookmarkLocation("Cached Name")).thenReturn(true)

            val initialPlace =
                Place().apply {
                    entityID = "Q1"
                    name = "Initial Name"
                    location = LatLng(1.0, 1.0, 0f)
                }
            val groups = mutableListOf(MarkerPlaceGroup(false, initialPlace))
            val indicesToUpdate = mutableListOf<Int>()

            loader.loadCachedPlaces(groups, indicesToUpdate)

            assertTrue(indicesToUpdate.isEmpty())
            assertTrue(groups[0].isBookmarked)
            assertEquals("Cached Name", groups[0].place.name)
            assertEquals("Cached Desc", groups[0].place.longDescription)
        }

    @Test
    fun testLoadCachedPlacesWhenNotFoundInCache() =
        runBlocking {
            whenever(placesRepository.fetchPlace("Q2")).thenReturn(null)

            val place =
                Place().apply {
                    entityID = "Q2"
                    name = "Place 2"
                    location = LatLng(2.0, 2.0, 0f)
                }
            val groups = mutableListOf(MarkerPlaceGroup(false, place))
            val indicesToUpdate = mutableListOf<Int>()

            loader.loadCachedPlaces(groups, indicesToUpdate)

            assertEquals(1, indicesToUpdate.size)
            assertEquals(0, indicesToUpdate[0])
            assertFalse(groups[0].isBookmarked)
        }

    @Test
    fun testSchedulePlacesUpdateWhenEmptyList() =
        runBlocking {
            loader.schedulePlacesUpdate(emptyList(), force = true)
            verifyNoInteractions(view)
        }

    @Test
    fun testSchedulePlacesUpdateForceRendersToView() =
        runBlocking {
            val place =
                Place().apply {
                    name = "Place 1"
                    location = LatLng(0.0, 0.0, 0f)
                    entityID = "Q1"
                }
            val groups = listOf(MarkerPlaceGroup(false, place))

            loader.schedulePlacesUpdate(groups, force = true)

            assertEquals(1, NearbyController.markerLabelList.size)
            assertEquals(groups[0], NearbyController.markerLabelList[0])
            verify(view).setFilterState()
            verify(view).updateListFragment(listOf(place))
            verifyNoMoreInteractions(view)
        }
}
