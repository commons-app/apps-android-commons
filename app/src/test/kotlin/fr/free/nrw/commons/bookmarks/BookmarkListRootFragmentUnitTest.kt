package fr.free.nrw.commons.bookmarks

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ListAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.tabs.TabLayout
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesFragment
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.ParentViewPager
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkListRootFragmentUnitTest {

    private lateinit var fragment: BookmarkListRootFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context
    private lateinit var activity: MainActivity
    private lateinit var view: View
    private lateinit var bookmarkFragment: BookmarkFragment

    @Mock
    private lateinit var listFragment: Fragment

    @Mock
    private lateinit var mediaDetails: MediaDetailPagerFragment

    @Mock
    private lateinit var childFragmentManager: FragmentManager

    @Mock
    private lateinit var childFragmentTransaction: FragmentTransaction

    @Mock
    private lateinit var bookmarksPagerAdapter: BookmarksPagerAdapter

    @Mock
    private lateinit var bundle: Bundle

    @Mock
    private lateinit var media: Media

    @Mock
    private lateinit var tabLayout: TabLayout

    @Mock
    private lateinit var viewPager: ParentViewPager

    @Mock
    private lateinit var adapter: BookmarksPagerAdapter

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        context = ApplicationProvider.getApplicationContext()

        fragment = BookmarkListRootFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        bookmarkFragment = BookmarkFragment()

        Whitebox.setInternalState(fragment, "mChildFragmentManager", childFragmentManager)
        Whitebox.setInternalState(fragment, "mParentFragment", bookmarkFragment)
        Whitebox.setInternalState(fragment, "listFragment", listFragment)
        Whitebox.setInternalState(fragment, "mediaDetails", mediaDetails)
        Whitebox.setInternalState(fragment, "bookmarksPagerAdapter", bookmarksPagerAdapter)
        Whitebox.setInternalState(bookmarkFragment, "tabLayout", tabLayout)
        Whitebox.setInternalState(bookmarkFragment, "viewPager", viewPager)
        Whitebox.setInternalState(bookmarkFragment, "adapter", adapter)

        whenever(childFragmentManager.beginTransaction()).thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.hide(any())).thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.add(ArgumentMatchers.anyInt(), any()))
            .thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.addToBackStack(any()))
            .thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.show(any())).thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.replace(ArgumentMatchers.anyInt(), any()))
            .thenReturn(childFragmentTransaction)
        whenever(childFragmentTransaction.remove(any())).thenReturn(childFragmentTransaction)

        layoutInflater = LayoutInflater.from(activity)
        view = fragment.onCreateView(layoutInflater, null, null) as View
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testConstructorCase0() {
        whenever(bundle.getInt("order")).thenReturn(0)
        fragment = BookmarkListRootFragment(bundle, bookmarksPagerAdapter)
        verify(bundle).getString("categoryName")
        verify(bundle).getInt("order")
        verify(bundle).getInt("orderItem")
    }

    @Test
    @Throws(Exception::class)
    fun testConstructorCase2() {
        whenever(bundle.getInt("order")).thenReturn(2)
        whenever(bundle.getInt("orderItem")).thenReturn(2)
        fragment = BookmarkListRootFragment(bundle, bookmarksPagerAdapter)
        verify(bundle).getString("categoryName")
        verify(bundle).getInt("order")
        verify(bundle).getInt("orderItem")
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewCreated() {
        fragment.onViewCreated(view, null)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).hide(mediaDetails)
        verify(childFragmentTransaction).add(R.id.explore_container, listFragment)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun `testSetFragment_Case fragment_isAdded && otherFragment != null`() {
        whenever(mediaDetails.isAdded).thenReturn(true)
        fragment.setFragment(mediaDetails, listFragment)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).hide(listFragment)
        verify(childFragmentTransaction).show(mediaDetails)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun `testSetFragment_Case fragment_isAdded && otherFragment == null`() {
        whenever(mediaDetails.isAdded).thenReturn(true)
        fragment.setFragment(mediaDetails, null)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).show(mediaDetails)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun `testSetFragment_Case fragment_isNotAdded && otherFragment != null`() {
        whenever(mediaDetails.isAdded).thenReturn(false)
        fragment.setFragment(mediaDetails, listFragment)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).hide(listFragment)
        verify(childFragmentTransaction).add(R.id.explore_container, mediaDetails)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun `testSetFragment_Case fragment_isNotAdded && otherFragment == null`() {
        whenever(mediaDetails.isAdded).thenReturn(false)
        fragment.setFragment(mediaDetails, null)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).replace(R.id.explore_container, mediaDetails)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPositionCaseNull() {
        whenever(bookmarksPagerAdapter.mediaAdapter).thenReturn(null)
        Assert.assertEquals(fragment.getMediaAtPosition(0), null)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPositionCaseNonNull() {
        val listAdapter = mock(ListAdapter::class.java)
        whenever(bookmarksPagerAdapter.mediaAdapter).thenReturn(listAdapter)
        whenever(listAdapter.getItem(0)).thenReturn(media)
        Assert.assertEquals(fragment.getMediaAtPosition(0), media)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCountCaseNull() {
        whenever(bookmarksPagerAdapter.mediaAdapter).thenReturn(null)
        Assert.assertEquals(fragment.totalMediaCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCountCaseNonNull() {
        val listAdapter = mock(ListAdapter::class.java)
        whenever(bookmarksPagerAdapter.mediaAdapter).thenReturn(listAdapter)
        whenever(listAdapter.count).thenReturn(1)
        Assert.assertEquals(fragment.totalMediaCount, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        Assert.assertEquals(fragment.getContributionStateAt(0), null)
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshNominatedMedia() {
        whenever(listFragment.isVisible).thenReturn(false)
        fragment.refreshNominatedMedia(0)
        verify(childFragmentManager, times(2)).beginTransaction()
        verify(childFragmentTransaction).remove(mediaDetails)
        verify(childFragmentTransaction, times(2)).commit()
        verify(childFragmentManager, times(2)).executePendingTransactions()
        verify(childFragmentTransaction).hide(listFragment)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        fragment.viewPagerNotifyDataSetChanged()
        verify(mediaDetails).notifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaClicked() {
        fragment.onMediaClicked(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackStackChanged() {
        fragment.onBackStackChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnItemClick() {
        fragment.onItemClick(null, null, 0, 0)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
        verify(childFragmentTransaction).hide(listFragment)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed Case NonNull isVisible and backButton clicked`() {
        whenever(mediaDetails.isVisible).thenReturn(true)
        Assert.assertEquals(fragment.backPressed(), false)
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed Case NonNull isVisible and backButton not clicked`() {
        Whitebox.setInternalState(fragment, "listFragment", mock(BookmarkPicturesFragment::class.java))
        whenever(mediaDetails.isVisible).thenReturn(true)
        whenever(mediaDetails.removedItems).thenReturn(ArrayList(0))
        Assert.assertEquals(fragment.backPressed(), false)
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed Case NonNull isNotVisible and backButton clicked`() {
        whenever(mediaDetails.isVisible).thenReturn(false)
        Assert.assertEquals(fragment.backPressed(), false)
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed Case Null`() {
        val field: Field = BookmarkListRootFragment::class.java.getDeclaredField("mediaDetails")
        field.isAccessible = true
        field.set(fragment, null)
        Assert.assertEquals(fragment.backPressed(), false)
    }

}