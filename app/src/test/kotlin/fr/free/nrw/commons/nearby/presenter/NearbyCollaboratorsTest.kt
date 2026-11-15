package fr.free.nrw.commons.nearby.presenter

import android.view.View
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import io.reactivex.Completable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Unit test suite for extracted collaborators: [NearbyUiInteractionHandler],
 * [NearbyFilterHandler], and [NearbyBookmarkCacheHandler].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearbyCollaboratorsTest {
    @Mock
    internal lateinit var view: NearbyParentFragmentContract.View

    @Mock
    internal lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Mock
    internal lateinit var placesRepository: PlacesRepository

    @Mock
    internal lateinit var placeDetailsLoader: NearbyPlaceDetailsLoader

    @Mock
    internal lateinit var kvStore: JsonKvStore

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUiHandlerAttachAndDetachView() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        assertTrue(uiHandler.nearbyParentFragmentView === view)

        uiHandler.detachView()
        assertTrue(uiHandler.nearbyParentFragmentView === NearbyUiInteractionHandler.DUMMY)
    }

    @Test
    fun testUiHandlerSetActionListenersFABPlusLoginSkipped() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(kvStore.getBoolean("login_skipped", false)).thenReturn(true)

        val captor = argumentCaptor<View.OnClickListener>()
        uiHandler.setActionListeners(kvStore)
        verify(view).setFABPlusAction(captor.capture())

        captor.firstValue.onClick(null)
        verify(view).displayLoginSkippedWarning()
    }

    @Test
    fun testUiHandlerSetActionListenersFABPlusNormal() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(kvStore.getBoolean("login_skipped", false)).thenReturn(false)

        val captor = argumentCaptor<View.OnClickListener>()
        uiHandler.setActionListeners(kvStore)
        verify(view).setFABPlusAction(captor.capture())

        captor.firstValue.onClick(null)
        verify(view).animateFABs()
    }

    @Test
    fun testUiHandlerSetActionListenersRecenter() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        val latLng = LatLng(10.0, 20.0, 0f)
        uiHandler.currentLatLngProvider = { latLng }

        val captor = argumentCaptor<View.OnClickListener>()
        uiHandler.setActionListeners(kvStore)
        verify(view).setFABRecenterAction(captor.capture())

        captor.firstValue.onClick(null)
        verify(view).recenterMap(latLng)
    }

    @Test
    fun testUiHandlerRemoveNearbyPreferences() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.removeNearbyPreferences(kvStore)
        verify(kvStore).remove(PLACE_OBJECT)
    }

    @Test
    fun testUiHandlerBackButtonClickedAdvancedQuery() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(view.isAdvancedQueryFragmentVisible()).thenReturn(true)

        assertTrue(uiHandler.backButtonClicked())
        verify(view).showHideAdvancedQueryFragment(false)
    }

    @Test
    fun testUiHandlerBackButtonClickedListBottomSheet() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(view.isListBottomSheetExpanded()).thenReturn(true)

        assertTrue(uiHandler.backButtonClicked())
        verify(view).listOptionMenuItemClicked()
    }

    @Test
    fun testUiHandlerBackButtonClickedDetailsBottomSheet() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(view.isDetailsBottomSheetVisible()).thenReturn(true)

        assertTrue(uiHandler.backButtonClicked())
        verify(view).setBottomSheetDetailsSmaller()
    }

    @Test
    fun testUiHandlerBackButtonClickedFilterList() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        whenever(view.isNearbyFilterListVisible()).thenReturn(true)

        assertTrue(uiHandler.backButtonClicked())
        verify(view).hideNearbyFilterList()
    }

    @Test
    fun testUiHandlerBackButtonClickedNone() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        assertFalse(uiHandler.backButtonClicked())
    }

    @Test
    fun testUiHandlerSearchViewGainedFocus() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)

        whenever(view.isListBottomSheetExpanded()).thenReturn(true)
        uiHandler.searchViewGainedFocus()
        verify(view).hideBottomSheet()

        whenever(view.isListBottomSheetExpanded()).thenReturn(false)
        whenever(view.isDetailsBottomSheetVisible()).thenReturn(true)
        uiHandler.searchViewGainedFocus()
        verify(view).hideBottomDetailsSheet()
    }

    @Test
    fun testUiHandlerMarkerUnselected() {
        val uiHandler = NearbyUiInteractionHandler()
        uiHandler.attachView(view)
        uiHandler.markerUnselected()
        verify(view).hideBottomSheet()
    }

    @Test
    fun testFilterHandlerSetCheckboxUnknown() {
        val filterHandler = NearbyFilterHandler { view }
        filterHandler.setCheckboxUnknown()
        verify(view).setCheckBoxState(CheckBoxTriStates.UNKNOWN)
    }

    @Test
    fun testFilterHandlerFilterByMarkerTypeChecked() {
        val filterHandler = NearbyFilterHandler { view }
        val labels = listOf<Label?>(Label.BUILDING)
        filterHandler.filterByMarkerType(
            labels,
            CheckBoxTriStates.CHECKED,
            filterForPlaceState = true,
            filterForAllNoneType = true,
        )
        verify(view).filterMarkersByLabels(labels, true, false)
        verify(view).setRecyclerViewAdapterAllSelected()
    }

    @Test
    fun testFilterHandlerFilterByMarkerTypeUnchecked() {
        val filterHandler = NearbyFilterHandler { view }
        val labels = listOf<Label?>(Label.BUILDING)
        filterHandler.filterByMarkerType(
            labels,
            CheckBoxTriStates.UNCHECKED,
            filterForPlaceState = true,
            filterForAllNoneType = true,
        )
        verify(view).filterOutAllMarkers()
        verify(view).setRecyclerViewAdapterItemsGreyedOut()
    }

    @Test
    fun testFilterHandlerFilterByMarkerTypeNormal() {
        val filterHandler = NearbyFilterHandler { view }
        val labels = listOf<Label?>(Label.BUILDING)
        filterHandler.filterByMarkerType(
            labels,
            CheckBoxTriStates.CHECKED,
            filterForPlaceState = false,
            filterForAllNoneType = false,
        )
        verify(view).filterMarkersByLabels(labels, false, false)
    }

    @Test
    fun testBookmarkCacheHandlerWikidataEditSuccessful() {
        var callbackCalled = false
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
                updateMapAndList = { callbackCalled = true },
            )
        bookmarkCacheHandler.onWikidataEditSuccessful()
        assertTrue(callbackCalled)
    }

    @Test
    fun testBookmarkCacheHandlerHandlePinClicked() {
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
            )
        val place = Place()
        bookmarkCacheHandler.handlePinClicked(place)
        verify(placeDetailsLoader).handlePinClicked(place)
    }

    @Test
    fun testBookmarkCacheHandlerLoadPlacesDataAsync() {
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
            )
        val groups = emptyList<MarkerPlaceGroup>()
        bookmarkCacheHandler.loadPlacesDataAsync(groups, null)
        verify(placeDetailsLoader).loadPlacesDataAsync(groups, null)
    }

    @Test
    fun testBookmarkCacheHandlerUpdateMapMarkers() {
        var lockedState: Boolean? = null
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
                lockNearby = { lockedState = it },
            )
        val place1 =
            Place().apply {
                name = "P1"
                location = LatLng(1.0, 1.0, 0f)
            }
        val currentLatLng = LatLng(1.0, 1.0, 0f)

        val groupsCaptor = argumentCaptor<List<MarkerPlaceGroup>>()
        bookmarkCacheHandler.updateMapMarkers(listOf(place1), currentLatLng, null)
        assertFalse(lockedState!!)
        verify(view).setProgressBarVisibility(false)
        verify(placeDetailsLoader).loadPlacesDataAsync(groupsCaptor.capture(), isNull())
        assertTrue(groupsCaptor.firstValue.size == 1)
    }

    @Test
    fun testBookmarkCacheHandlerRemoveOldCachedPlacesSkipped() {
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
            )
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        val today = TimeUnit.MILLISECONDS.toDays(now + offset)
        whenever(kvStore.getLong("last_cached_places_removal_day", -1L)).thenReturn(today)

        bookmarkCacheHandler.removeOldCachedPlaces(kvStore)
        verify(placesRepository, never()).removePlacesCachedBefore(any())
    }

    @Test
    fun testBookmarkCacheHandlerRemoveOldCachedPlacesExecuted() {
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
            )
        whenever(kvStore.getLong("last_cached_places_removal_day", -1L)).thenReturn(-1L)
        whenever(placesRepository.removePlacesCachedBefore(any()))
            .thenReturn(Completable.complete())

        bookmarkCacheHandler.removeOldCachedPlaces(kvStore)
        verify(placesRepository).removePlacesCachedBefore(any())
        verify(kvStore).putLong(
            eq("last_cached_places_removal_day"),
            any(),
        )
    }

    @Test
    fun testBookmarkCacheHandlerSearchCloseToCurrentLocationNullLastFocus() {
        val bookmarkCacheHandler =
            NearbyBookmarkCacheHandler(
                bookmarkLocationDao,
                placesRepository,
                placeDetailsLoader,
                { view },
            )
        whenever(view.getLastMapFocus()).thenReturn(null)
        assertTrue(bookmarkCacheHandler.searchCloseToCurrentLocation())
    }
}
