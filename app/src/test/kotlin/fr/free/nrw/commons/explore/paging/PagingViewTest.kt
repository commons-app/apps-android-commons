package fr.free.nrw.commons.explore.paging

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentSearchPaginatedBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.clearInvocations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Exercises query replacement and view recreation independently of presenter network subscriptions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class PagingViewTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fragment: BasePagingFragment<String> = mock()
    private val adapter: PagedListAdapter<String, *> = mock()
    private lateinit var owner: PagingLifecycleOwner
    private lateinit var binding: FragmentSearchPaginatedBinding
    private lateinit var pagingView: PagingView<String>

    /** Supplies a live view owner while avoiding fragment injection and presenter initialization. */
    @Before
    fun setUp() {
        owner = PagingLifecycleOwner()
        owner.moveTo(Lifecycle.State.STARTED)
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.LightAppTheme)
        binding = FragmentSearchPaginatedBinding.inflate(LayoutInflater.from(context))
        whenever(fragment.viewLifecycleOwner).thenReturn(owner)
        whenever(fragment.pagedListAdapter).thenReturn(adapter)
        pagingView = PagingView()
        pagingView.bind(fragment) { binding }
    }

    /** Initial-page progress remains independent from the list footer. */
    @Test
    fun initialLoadVisibilityCanBeShownAndHidden() {
        pagingView.hideInitialLoadProgress()
        assertEquals(GONE, binding.paginatedSearchInitialLoadProgress.visibility)
        pagingView.showInitialLoadInProgress()
        assertEquals(VISIBLE, binding.paginatedSearchInitialLoadProgress.visibility)
    }

    /** Different result types retain control over the message used for an empty query result. */
    @Test
    fun emptyMessageUsesFragmentFormatting() {
        whenever(fragment.getEmptyText("cats")).thenReturn("No cats found")
        pagingView.showEmptyText("cats")
        assertEquals("No cats found", binding.contentNotFound.text.toString())
        assertEquals(VISIBLE, binding.contentNotFound.visibility)
        pagingView.hideEmptyText()
        assertEquals(GONE, binding.contentNotFound.visibility)
    }

    /** Results from an older query must not overwrite a newer query's list. */
    @Test
    fun replacingQueryRemovesPreviousObserver() {
        val previous = MutableLiveData<PagedList<String>>()
        val current = MutableLiveData<PagedList<String>>()
        val previousItems: PagedList<String> = mock()
        val currentItems: PagedList<String> = mock()
        pagingView.observePagingResults(previous)
        previous.value = previousItems
        verify(adapter).submitList(previousItems)
        clearInvocations(adapter)

        pagingView.observePagingResults(current)
        assertFalse(previous.hasObservers())
        previous.value = previousItems
        current.value = currentItems
        verify(adapter).submitList(currentItems)
        verifyNoMoreInteractions(adapter)
    }

    /** A destroyed fragment view stops receiving list updates through LiveData. */
    @Test
    fun destroyingViewRemovesObserver() {
        val results = MutableLiveData<PagedList<String>>()
        pagingView.observePagingResults(results)
        owner.moveTo(Lifecycle.State.DESTROYED)
        assertFalse(results.hasObservers())
        results.value = mock()
        verifyNoMoreInteractions(adapter)
    }

    /** The fragment's new view lifecycle is used when results are observed after recreation. */
    @Test
    fun recreatedViewUsesNewLifecycleOwner() {
        val previous = MutableLiveData<PagedList<String>>()
        pagingView.observePagingResults(previous)
        owner.moveTo(Lifecycle.State.DESTROYED)
        owner = PagingLifecycleOwner()
        owner.moveTo(Lifecycle.State.STARTED)
        whenever(fragment.viewLifecycleOwner).thenReturn(owner)
        val current = MutableLiveData<PagedList<String>>()
        val items: PagedList<String> = mock()
        pagingView.observePagingResults(current)
        current.value = items
        verify(adapter).submitList(items)
        owner.moveTo(Lifecycle.State.DESTROYED)
        assertFalse(current.hasObservers())
    }

    /** UI callbacks must resolve the replacement binding instead of retaining the old view. */
    @Test
    fun recreatedViewUsesNewBinding() {
        val oldBinding = binding
        oldBinding.paginatedSearchInitialLoadProgress.visibility = GONE
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.LightAppTheme)
        binding = FragmentSearchPaginatedBinding.inflate(LayoutInflater.from(context))
        binding.paginatedSearchInitialLoadProgress.visibility = GONE
        pagingView.showInitialLoadInProgress()
        assertEquals(VISIBLE, binding.paginatedSearchInitialLoadProgress.visibility)
        assertEquals(GONE, oldBinding.paginatedSearchInitialLoadProgress.visibility)
    }
}

/** Provides a controllable fragment-view lifecycle for observer removal tests. */
private class PagingLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = registry

    /** Moves the view between active and destroyed states without creating an activity. */
    fun moveTo(state: Lifecycle.State) {
        registry.currentState = state
    }
}
