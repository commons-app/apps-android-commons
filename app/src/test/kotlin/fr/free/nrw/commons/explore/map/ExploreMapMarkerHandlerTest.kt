package fr.free.nrw.commons.explore.map

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import org.junit.Before
import org.junit.Test

/** Checks asynchronous marker completion and its connection to presenter attachment and locking. */
class ExploreMapMarkerHandlerTest {
    private val initialView: ExploreMapContract.View = mock()
    private val lockUnlock: (Boolean) -> Unit = mock()
    private var currentView: ExploreMapContract.View? = initialView
    private lateinit var handler: ExploreMapMarkerHandler

    /** Keeps the view provider mutable to model detachment and reattachment during a search. */
    @Before
    fun setUp() {
        handler = ExploreMapMarkerHandler()
        handler.bind({ currentView }, lockUnlock)
    }

    /** Markers must be displayed before the progress indicator is cleared and searches resume. */
    @Test
    fun completionAddsMarkersAndFinishesLoading() {
        val markers = listOf(mock<BaseMarker>())
        handler.onNearbyBaseMarkerThumbsReady(markers, null)
        inOrder(initialView, lockUnlock) {
            verify(initialView).addMarkersToMap(markers)
            verify(lockUnlock).invoke(false)
            verify(initialView).setProgressBarVisibility(false)
        }
    }

    /** A completed request must use the current view rather than the view that started loading. */
    @Test
    fun completionUsesReplacementView() {
        val replacement: ExploreMapContract.View = mock()
        currentView = replacement
        val markers = listOf(mock<BaseMarker>())
        handler.onNearbyBaseMarkerThumbsReady(markers, null)
        verify(replacement).addMarkersToMap(markers)
        verify(replacement).setProgressBarVisibility(false)
        verify(lockUnlock).invoke(false)
        verifyNoMoreInteractions(initialView)
    }

    /** An explicitly absent view retains the callback's existing no-op behavior. */
    @Test
    fun completionWithoutViewDoesNotUnlockOrUpdateUi() {
        currentView = null
        handler.onNearbyBaseMarkerThumbsReady(emptyList(), null)
        verifyNoMoreInteractions(initialView, lockUnlock)
    }

    /** Nullable callback data continues to be forwarded without changing its meaning. */
    @Test
    fun completionForwardsNullMarkers() {
        handler.onNearbyBaseMarkerThumbsReady(null, null)
        verify(initialView).addMarkersToMap(null)
        verify(initialView).setProgressBarVisibility(false)
        verify(lockUnlock).invoke(false)
    }

    /** The presenter's dummy view must absorb callbacks that arrive after detachment. */
    @Test
    fun presenterDoesNotUpdateDetachedView() {
        val presenter = ExploreMapPresenter(mock())
        presenter.attachView(initialView)
        presenter.detachView()
        presenter.onNearbyBaseMarkerThumbsReady(emptyList(), null)
        verifyNoMoreInteractions(initialView)
    }

    /** Delegated completion must release the same lock that blocks overlapping map searches. */
    @Test
    fun presenterCanSearchAgainAfterMarkerCompletion() {
        val presenter = ExploreMapPresenter(mock())
        presenter.attachView(initialView)
        whenever(initialView.isNetworkConnectionEstablished()).thenReturn(true)

        presenter.updateMap(LocationChangeType.SEARCH_CUSTOM_AREA)
        presenter.updateMap(LocationChangeType.SEARCH_CUSTOM_AREA)
        verify(initialView).populatePlaces(null)

        presenter.onNearbyBaseMarkerThumbsReady(emptyList(), null)
        presenter.updateMap(LocationChangeType.SEARCH_CUSTOM_AREA)
        verify(initialView, times(2)).populatePlaces(null)
    }
}
