package fr.free.nrw.commons.explore.search

import android.view.View
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.ActivitySearchBinding
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.SearchQueryController
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment
import fr.free.nrw.commons.explore.media.SearchMediaFragment
import fr.free.nrw.commons.explore.models.RecentSearch
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.Date

/**
 * Checks query/history behavior independently of the activity's media-detail navigation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchQueryControllerTest {
    private lateinit var activity: SearchActivity
    private lateinit var controller: SearchQueryController
    private lateinit var binding: ActivitySearchBinding
    private lateinit var dao: RecentSearchesDao
    private lateinit var media: SearchMediaFragment
    private lateinit var categories: SearchCategoryFragment
    private lateinit var depictions: SearchDepictionsFragment
    private lateinit var history: RecentSearchesFragment

    /**
     * Keeps result views active without starting network requests from their real presenters.
     */
    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()
        controller = Whitebox.getInternalState(activity, "searchQueryController")
        binding = Whitebox.getInternalState(activity, "binding")
        dao = mock()
        activity.recentSearchesDao = dao
        media = mock()
        categories = mock()
        depictions = mock()
        history = mock()
        Whitebox.setInternalState(controller, "searchMediaFragment", media)
        Whitebox.setInternalState(controller, "searchCategoryFragment", categories)
        Whitebox.setInternalState(controller, "searchDepictionsFragment", depictions)
        Whitebox.setInternalState(controller, "recentSearchesFragment", history)
        listOf(media, categories, depictions).forEach {
            whenever(it.activity).thenReturn(activity)
            whenever(it.isAdded).thenReturn(true)
        }
    }

    /**
     * A new query retains its exact text and is inserted with the time it was searched.
     */
    @Test
    fun newQueryCreatesHistoryEntry() {
        val before = Date()
        controller.handleSearch(" Sunset ")
        val saved = argumentCaptor<RecentSearch>()
        verify(dao).save(saved.capture())
        assertEquals(" Sunset ", saved.firstValue.query)
        assertNull(saved.firstValue.contentUri)
        assertTrue(saved.firstValue.lastSearched >= before)
        assertTrue(saved.firstValue.lastSearched <= Date())
    }

    /**
     * Repeating a query updates the same record rather than inserting a duplicate history entry.
     */
    @Test
    fun repeatedQueryUpdatesExistingEntry() {
        val existing = RecentSearch(null, "cat", Date(0))
        whenever(dao.find("cat")).thenReturn(existing)
        val before = Date()
        controller.handleSearch("cat")
        val saved = argumentCaptor<RecentSearch>()
        verify(dao).save(saved.capture())
        assertSame(existing, saved.firstValue)
        assertTrue(existing.lastSearched >= before)
    }

    /**
     * Clearing a query refreshes history without persisting an empty search.
     */
    @Test
    fun emptyQueryShowsHistory() {
        controller.handleSearch("")
        verify(history).updateRecentSearches()
        verify(dao, never()).find(any())
        verify(dao, never()).save(any())
        assertEquals(View.GONE, binding.viewPager.visibility)
        assertEquals(View.GONE, binding.tabLayout.visibility)
        assertEquals(View.VISIBLE, binding.searchHistoryContainer.visibility)
    }

    /**
     * The same settled query reaches every active tab while results replace the history view.
     */
    @Test
    fun nonemptyQueryUpdatesAllActiveTabs() {
        controller.handleSearch("cat")
        verify(media).onQueryUpdated("cat")
        verify(categories).onQueryUpdated("cat")
        verify(depictions).onQueryUpdated("cat")
        assertEquals(View.VISIBLE, binding.viewPager.visibility)
        assertEquals(View.VISIBLE, binding.tabLayout.visibility)
        assertEquals(View.GONE, binding.searchHistoryContainer.visibility)
    }

    /**
     * Query dispatch does not access result views that are detached, removing, or not yet added.
     */
    @Test
    fun inactiveTabsDoNotReceiveQueries() {
        whenever(media.isDetached).thenReturn(true)
        whenever(categories.isAdded).thenReturn(false)
        whenever(depictions.isRemoving).thenReturn(true)
        controller.handleSearch("cat")
        verify(media, never()).onQueryUpdated(any())
        verify(categories, never()).onQueryUpdated(any())
        verify(depictions, never()).onQueryUpdated(any())
        verify(dao).find("cat")
    }
}
