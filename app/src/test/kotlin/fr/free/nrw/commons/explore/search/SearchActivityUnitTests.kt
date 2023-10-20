package fr.free.nrw.commons.explore.search

import android.content.Context
import android.widget.SearchView
import androidx.fragment.app.FragmentController
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager.widget.ViewPager
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.explore.categories.search.SearchCategoryFragment
import fr.free.nrw.commons.explore.depictions.search.SearchDepictionsFragment
import fr.free.nrw.commons.explore.media.SearchMediaFragment
import fr.free.nrw.commons.explore.models.RecentSearch
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchActivityUnitTests {

    @Mock
    private lateinit var activity: SearchActivity

    @Mock
    private lateinit var searchView: SearchView

    @Mock
    private lateinit var viewPager: ViewPager

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var mediaDetails: MediaDetailPagerFragment

    @Mock
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    @Mock
    private lateinit var recentSearchesDao: RecentSearchesDao

    @Mock
    private lateinit var searchMediaFragment: SearchMediaFragment

    @Mock
    private lateinit var recentSearchesFragment: RecentSearchesFragment

    @Mock
    private lateinit var supportFragmentManager: FragmentManager

    @Mock
    private lateinit var searchDepictionsFragment: SearchDepictionsFragment

    @Mock
    private lateinit var searchCategoryFragment: SearchCategoryFragment

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetTabs() {
        Whitebox.setInternalState(activity, "viewPagerAdapter", viewPagerAdapter)
        activity.setTabs()
        verify(viewPagerAdapter).notifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateText() {
        val query = "test"
        Whitebox.setInternalState(activity, "searchView", searchView)
        Whitebox.setInternalState(activity, "viewPager", viewPager)
        activity.updateText(query)
        verify(searchView).setQuery(query, true)
        verify(viewPager).requestFocus()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        Whitebox.setInternalState(activity, "mediaDetails", mediaDetails)
        activity.viewPagerNotifyDataSetChanged()
        verify(mediaDetails).notifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        assertNull(activity.getContributionStateAt(0))
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        Whitebox.setInternalState(activity, "compositeDisposable", compositeDisposable)
        val method: Method = SearchActivity::class.java.getDeclaredMethod("onDestroy")
        method.isAccessible = true
        method.invoke(activity)
        verify(compositeDisposable).dispose()
    }

    @Test
    @Throws(Exception::class)
    fun testSaveRecentSearchCaseNull() {
        val query = "test"
        Whitebox.setInternalState(activity, "recentSearchesDao", recentSearchesDao)
        val method: Method =
            SearchActivity::class.java.getDeclaredMethod("saveRecentSearch", String::class.java)
        method.isAccessible = true
        method.invoke(activity, query)
        verify(recentSearchesDao).find(query)
    }

    @Test
    @Throws(Exception::class)
    fun testSaveRecentSearchCaseNonNull() {
        val query = "test"
        Whitebox.setInternalState(activity, "recentSearchesDao", recentSearchesDao)
        `when`(recentSearchesDao.find(query)).thenReturn(mock(RecentSearch::class.java))
        val method: Method =
            SearchActivity::class.java.getDeclaredMethod("saveRecentSearch", String::class.java)
        method.isAccessible = true
        method.invoke(activity, query)
        verify(recentSearchesDao).find(query)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPosition() {
        val pos = 0
        val media = mock(Media::class.java)
        Whitebox.setInternalState(activity, "searchMediaFragment", searchMediaFragment)
        `when`(searchMediaFragment.getMediaAtPosition(pos)).thenReturn(media)
        assertEquals(activity.getMediaAtPosition(pos), media)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        val num = 1
        Whitebox.setInternalState(activity, "searchMediaFragment", searchMediaFragment)
        `when`(searchMediaFragment.totalMediaCount).thenReturn(num)
        assertEquals(activity.totalMediaCount, num)
    }

    @Test
    @Throws(Exception::class)
    fun testHandleSearchCaseEmpty() {
        Whitebox.setInternalState(activity, "recentSearchesFragment", recentSearchesFragment)
        val query = ""
        val method: Method = SearchActivity::class.java.getDeclaredMethod(
            "handleSearch",
            CharSequence::class.java
        )
        method.isAccessible = true
        method.invoke(activity, query)
        verify(recentSearchesFragment).updateRecentSearches()
    }

    @Test
    @Throws(Exception::class)
    fun testHandleSearchCaseNotEmpty() {
        val query = "test"
        Whitebox.setInternalState(activity, "recentSearchesDao", recentSearchesDao)
        Whitebox.setInternalState(activity, "searchDepictionsFragment", searchDepictionsFragment)
        Whitebox.setInternalState(activity, "searchMediaFragment", searchMediaFragment)
        Whitebox.setInternalState(activity, "searchCategoryFragment", searchCategoryFragment)

        `when`(searchDepictionsFragment.activity).thenReturn(activity)
        `when`(searchMediaFragment.activity).thenReturn(activity)
        `when`(searchCategoryFragment.activity).thenReturn(activity)

        `when`(searchDepictionsFragment.isAdded).thenReturn(true)
        `when`(searchMediaFragment.isAdded).thenReturn(true)
        `when`(searchCategoryFragment.isAdded).thenReturn(true)

        `when`(searchDepictionsFragment.isDetached).thenReturn(false)
        `when`(searchMediaFragment.isDetached).thenReturn(false)
        `when`(searchCategoryFragment.isDetached).thenReturn(false)

        `when`(searchDepictionsFragment.isRemoving).thenReturn(false)
        `when`(searchMediaFragment.isRemoving).thenReturn(false)
        `when`(searchCategoryFragment.isRemoving).thenReturn(false)

        val method: Method = SearchActivity::class.java.getDeclaredMethod(
            "handleSearch",
            CharSequence::class.java
        )
        method.isAccessible = true
        method.invoke(activity, query)
        verify(recentSearchesDao).find(query)
        verify(searchDepictionsFragment).onQueryUpdated(query)
        verify(searchMediaFragment).onQueryUpdated(query)
        verify(searchCategoryFragment).onQueryUpdated(query)
    }

}