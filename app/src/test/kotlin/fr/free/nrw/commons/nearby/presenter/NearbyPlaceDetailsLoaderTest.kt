package fr.free.nrw.commons.nearby.presenter

import androidx.lifecycle.LifecycleCoroutineScope
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.timeout
import com.nhaarman.mockitokotlin2.times
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
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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

    @Test
    fun testLoadPlacesDataAsyncMatchesRowsToPlacesByQid() =
        runBlocking {
            stubForLoading()
            val groups = listOf(greyGroup("Q1"), greyGroup("Q2"))
            // Wikidata gives one row per class, and not in the order the places were requested
            whenever(nearbyController.getPlaces(any())).thenReturn(
                listOf(fetchedPlace("Q2"), fetchedPlace("Q1"), fetchedPlace("Q1")),
            )

            loader.loadPlacesDataAsync(groups, scopeWith(Job()))
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            verify(nearbyController, times(1)).getPlaces(any())
            assertEquals(
                listOf("Name of Q1", "Name of Q2"),
                NearbyController.markerLabelList.map { it.place.name },
            )
        }

    @Test
    fun testLoadPlacesDataAsyncProcessesBatchesAsTheyArrive() =
        runBlocking<Unit> {
            stubForLoading()
            // 11 places make one batch of 10 and one batch of 1
            val groups = (0..10).map { greyGroup("Q$it") }
            val secondBatchCanAnswer = CountDownLatch(1)
            whenever(nearbyController.getPlaces(any())).thenAnswer {
                val places = it.getArgument<List<Place>>(0)
                if (places.size == 1) secondBatchCanAnswer.await(10, TimeUnit.SECONDS)
                places.map { place -> fetchedPlace(place.entityID) }
            }

            loader.loadPlacesDataAsync(groups, scopeWith(Job()))
            try {
                verify(placesRepository, timeout(5000).times(10)).save(any())
            } finally {
                secondBatchCanAnswer.countDown()
            }
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            verify(placesRepository, times(11)).save(any())
        }

    @Test
    fun testFailedRequestsDoNotSaveEmptyPlacesToCache() =
        runBlocking<Unit> {
            stubForLoading()
            whenever(nearbyController.getPlaces(any())).thenThrow(IOException("Unexpected response code: 429"))

            loader.loadPlacesDataAsync(listOf(greyGroup("Q1"), greyGroup("Q2")), scopeWith(Job()))
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            assertEquals("places saved to the cache", 0, saveCount())
        }

    @Test
    fun testPlaceMissingFromAnswerStaysAsItIsAndIsNotCached() =
        runBlocking<Unit> {
            stubForLoading()
            whenever(nearbyController.getPlaces(any())).thenReturn(listOf(fetchedPlace("Q1")))

            loader.loadPlacesDataAsync(listOf(greyGroup("Q1"), greyGroup("Q2")), scopeWith(Job()))
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            assertEquals(listOf("Name of Q1", ""), NearbyController.markerLabelList.map { it.place.name })
            assertEquals("places saved to the cache", 1, saveCount())
        }

    @Test
    fun testSameItemTwiceKeepsEachPinAtItsOwnLocation() =
        runBlocking<Unit> {
            stubForLoading()
            // a custom query can return the same item at two locations
            val groups = listOf(greyGroup("Q1"), greyGroup("Q1").apply { place.location = LatLng(5.0, 5.0, 0f) })
            whenever(nearbyController.getPlaces(any())).thenReturn(listOf(fetchedPlace("Q1")))

            loader.loadPlacesDataAsync(groups, scopeWith(Job()))
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            assertEquals(
                listOf(1.0, 5.0),
                NearbyController.markerLabelList.map { it.place.location.latitude },
            )
        }

    @Test
    fun testBookmarkMadeWhileItsBatchIsLoadingIsKept() =
        runBlocking<Unit> {
            Dispatchers.setMain(Dispatchers.Unconfined)
            // 20 places make batch A (Q0-Q9) and batch B (Q10-Q19)
            val groups = (0..19).map { greyGroup("Q$it") }
            val q10Bookmarked = AtomicBoolean(false)
            whenever(bookmarkLocationDao.findBookmarkLocation(any())).thenAnswer {
                it.getArgument<String>(0) == "Name of Q10" && q10Bookmarked.get()
            }
            // Hold the collector while it applies batch A, until batch B is fetched and waiting
            val collectorInA = CountDownLatch(1)
            val releaseCollector = CountDownLatch(1)
            val firstSave = AtomicBoolean(true)
            whenever(placesRepository.save(any())).thenAnswer {
                if (firstSave.getAndSet(false)) {
                    collectorInA.countDown()
                    releaseCollector.await(10, TimeUnit.SECONDS)
                }
                Completable.complete()
            }
            val batchBFetched = CountDownLatch(1)
            whenever(nearbyController.getPlaces(any())).thenAnswer {
                val places = it.getArgument<List<Place>>(0)
                if (places.any { place -> place.entityID == "Q10" }) {
                    collectorInA.await(10, TimeUnit.SECONDS)
                    batchBFetched.countDown()
                }
                places.map { place -> fetchedPlace(place.entityID) }
            }

            loader.loadPlacesDataAsync(groups, scopeWith(Job()))
            assertTrue("collector reached batch A", collectorInA.await(10, TimeUnit.SECONDS))
            assertTrue("batch B fetched", batchBFetched.await(10, TimeUnit.SECONDS))
            Thread.sleep(300) // batch B finishes on its worker and waits for the collector
            // the user opens pin Q10 and bookmarks it, batch A will apply this
            q10Bookmarked.set(true)
            val clicked = fetchedPlace("Q10").apply { location = groups[10].place.location }
            loader.handlePinClicked(clicked)
            loader.bookmarkChangedPlaces.add(clicked)
            releaseCollector.countDown()
            withTimeout(10_000) { loader.loadPlacesDataAyncJob!!.join() }

            assertTrue("Q10 still bookmarked", NearbyController.markerLabelList[10].isBookmarked)
        }

    private suspend fun stubForLoading() {
        // the real Main dispatcher, so that the delay in schedulePlacesUpdate ends
        Dispatchers.setMain(Dispatchers.Unconfined)
        whenever(placesRepository.save(any())).thenReturn(Completable.complete())
        whenever(bookmarkLocationDao.findBookmarkLocation(any())).thenReturn(false)
    }

    private fun saveCount() =
        Mockito
            .mockingDetails(placesRepository)
            .invocations
            .count { it.method.name == "save" }

    private fun scopeWith(job: Job): LifecycleCoroutineScope =
        mock<LifecycleCoroutineScope>().also {
            whenever(it.coroutineContext).thenReturn(job)
        }

    private fun greyGroup(qid: String) =
        MarkerPlaceGroup(
            false,
            Place().apply {
                entityID = qid
                name = ""
                location = LatLng(qid.drop(1).toDouble(), 0.0, 0f)
            },
        )

    private fun fetchedPlace(qid: String) =
        Place().apply {
            entityID = qid
            name = "Name of $qid"
        }
}
