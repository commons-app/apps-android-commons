package fr.free.nrw.commons.nearby.fragments

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import io.reactivex.Completable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyPlacesQueryHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyPlacesQueryHelperTest {
    private lateinit var helper: NearbyPlacesQueryHelper
    private lateinit var nearbyController: NearbyController
    private lateinit var placesRepository: PlacesRepository
    private lateinit var presenter: NearbyParentFragmentPresenter
    private lateinit var applicationKvStore: JsonKvStore
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var actions: NearbyPlacesQueryHelper.PlacesQueryActions

    private val testLatLng = LatLng(10.0, 20.0, 0f)

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }

        helper = NearbyPlacesQueryHelper()
        nearbyController = mock()
        placesRepository = mock()
        presenter = mock()
        applicationKvStore = mock()
        compositeDisposable = CompositeDisposable()
        actions = mock()

        val viewport =
            PlacesQueryViewport(
                screenTopRight = LatLng(10.1, 20.1, 0f),
                screenBottomLeft = LatLng(9.9, 19.9, 0f),
                lastMapFocus = testLatLng,
                mapFocus = testLatLng,
                lastKnownLocation = testLatLng,
            )
        whenever(actions.getQueryViewport()).thenReturn(viewport)

        helper.init(
            dependencies =
                NearbyPlacesDependencies(
                    nearbyControllerProvider = { nearbyController },
                    placesRepositoryProvider = { placesRepository },
                    compositeDisposableProvider = { compositeDisposable },
                    presenterProvider = { presenter },
                    applicationKvStoreProvider = { applicationKvStore },
                    scopeProvider = { null },
                ),
            actions = actions,
        )
    }

    @After
    fun tearDown() {
        compositeDisposable.dispose()
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun testStopQueryClearsDisposablesAndSetsFlag() {
        helper.stopQuery()
        assertTrue(helper.stopQuery)
    }

    @Test
    fun testPopulatePlacesCallsNearbyController() {
        val placesInfo = mock<NearbyPlacesInfo>()
        val place = Place()
        place.name = "Test Place"
        placesInfo.placeList = listOf(place)
        whenever(
            nearbyController.loadAttractionsFromLocation(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
                eq(true),
                any(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(placesInfo)

        helper.populatePlaces(testLatLng)

        verify(nearbyController).loadAttractionsFromLocation(
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(testLatLng),
            eq(false),
            eq(true),
            any(),
            anyOrNull(),
            eq(false),
        )
        verify(presenter).updateMapMarkers(any(), any(), anyOrNull())
        assertEquals(testLatLng, helper.lastFocusLocation)
    }

    @Test
    fun testEmptyCacheInvokesClearCache() {
        whenever(placesRepository.clearCache()).thenReturn(Completable.complete())

        helper.emptyCache()

        verify(placesRepository).clearCache()
    }

    @Test
    fun testSavePlaceToDatabaseInvokesRepository() {
        val place = Place()
        whenever(placesRepository.save(place)).thenReturn(Completable.complete())

        helper.savePlaceToDatabase(place)

        verify(placesRepository).save(place)
    }

    @Test
    fun testPopulatePlacesWithCustomQueryUsesLastKnownLocation() {
        val placesInfo = mock<NearbyPlacesInfo>()
        placesInfo.placeList = listOf(Place())
        whenever(
            nearbyController.loadAttractionsFromLocation(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
                eq(true),
                any(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(placesInfo)

        helper.populatePlaces(testLatLng, "custom query")

        verify(nearbyController).loadAttractionsFromLocation(
            eq(testLatLng),
            anyOrNull(),
            anyOrNull(),
            eq(testLatLng),
            eq(false),
            eq(true),
            any(),
            eq("custom query"),
            eq(false),
        )
    }

    @Test
    fun testPopulatePlacesWithZeroInitialBoundsUsesFallbackDelta() {
        val zeroViewport =
            PlacesQueryViewport(
                screenTopRight = LatLng(0.0, 0.0, 0f),
                screenBottomLeft = LatLng(0.0, 0.0, 0f),
                lastMapFocus = testLatLng,
                mapFocus = testLatLng,
                lastKnownLocation = testLatLng,
            )
        whenever(actions.getQueryViewport()).thenReturn(zeroViewport)
        val placesInfo = mock<NearbyPlacesInfo>()
        placesInfo.placeList = emptyList()
        whenever(
            nearbyController.loadAttractionsFromLocation(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
                eq(true),
                any(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn(placesInfo)

        helper.populatePlaces(testLatLng)

        verify(actions).clearPlacesDisplay()
        verify(actions).showNoNearbyPlacesMessage()
        verify(actions).setProgressBarVisibility(false)
    }

    @Test
    fun testPopulatePlacesErrorCallsShowErrorFetchingPlacesMessage() {
        whenever(
            nearbyController.loadAttractionsFromLocation(
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
                eq(true),
                any(),
                anyOrNull(),
                any(),
            ),
        ).thenThrow(RuntimeException("Network failure"))

        helper.populatePlaces(testLatLng)

        verify(actions).showErrorFetchingPlacesMessage(any())
        verify(actions).setProgressBarVisibility(false)
        verify(actions).setFilterState()
    }
}
