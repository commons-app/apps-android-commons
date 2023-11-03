package fr.free.nrw.commons.explore

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.tabs.TabLayout
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
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
class ExploreListRootFragmentUnitTest {

    private lateinit var fragment: ExploreListRootFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var activity: MainActivity
    private lateinit var exploreFragment: ExploreFragment

    @Mock
    private lateinit var mediaDetails: MediaDetailPagerFragment

    @Mock
    private lateinit var listFragment: CategoriesMediaFragment

    @Mock
    private lateinit var childFragmentManager: FragmentManager

    @Mock
    private lateinit var childFragmentTransaction: FragmentTransaction

    @Mock
    private lateinit var container: FrameLayout

    @Mock
    private lateinit var tabLayout: TabLayout

    @Mock
    private lateinit var viewPager: ParentViewPager

    @Mock
    private lateinit var media: Media

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        fragment = ExploreListRootFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        exploreFragment = ExploreFragment()

        layoutInflater = LayoutInflater.from(activity)
        view = fragment.onCreateView(layoutInflater, null, null) as View

        Whitebox.setInternalState(fragment, "mChildFragmentManager", childFragmentManager)
        Whitebox.setInternalState(fragment, "mParentFragment", exploreFragment)
        Whitebox.setInternalState(fragment, "mediaDetails", mediaDetails)
        Whitebox.setInternalState(fragment, "listFragment", listFragment)
        Whitebox.setInternalState(fragment, "container", container)
        Whitebox.setInternalState(exploreFragment, "tabLayout", tabLayout)
        Whitebox.setInternalState(exploreFragment, "viewPager", viewPager)

        `when`(childFragmentManager.beginTransaction()).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.hide(any())).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.add(anyInt(), any())).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.addToBackStack(any())).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.show(any())).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.replace(anyInt(), any())).thenReturn(childFragmentTransaction)
        `when`(childFragmentTransaction.remove(any())).thenReturn(childFragmentTransaction)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
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
        `when`(mediaDetails.isAdded).thenReturn(true)
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
        `when`(mediaDetails.isAdded).thenReturn(true)
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
        `when`(mediaDetails.isAdded).thenReturn(false)
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
        `when`(mediaDetails.isAdded).thenReturn(false)
        fragment.setFragment(mediaDetails, null)
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).replace(R.id.explore_container, mediaDetails)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaClicked() {
        fragment.onMediaClicked(0)
        verify(container).visibility = View.VISIBLE
        verify(tabLayout).visibility = View.GONE
        verify(childFragmentManager).beginTransaction()
        verify(childFragmentTransaction).hide(listFragment)
        verify(childFragmentTransaction).addToBackStack("CONTRIBUTION_LIST_FRAGMENT_TAG")
        verify(childFragmentTransaction).commit()
        verify(childFragmentManager).executePendingTransactions()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPosition() {
        `when`(listFragment.getMediaAtPosition(0)).thenReturn(media)
        Assert.assertEquals(fragment.getMediaAtPosition(0), media)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPositionCaseNull() {
        val field: Field = ExploreListRootFragment::class.java.getDeclaredField("listFragment")
        field.isAccessible = true
        field.set(fragment, null)
        Assert.assertEquals(fragment.getMediaAtPosition(0), null)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        `when`(listFragment.totalMediaCount).thenReturn(1)
        Assert.assertEquals(fragment.totalMediaCount, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCountCaseNull() {
        val field: Field = ExploreListRootFragment::class.java.getDeclaredField("listFragment")
        field.isAccessible = true
        field.set(fragment, null)
        Assert.assertEquals(fragment.totalMediaCount, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        Assert.assertEquals(fragment.getContributionStateAt(0), null)
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshNominatedMedia() {
        `when`(listFragment.isVisible).thenReturn(false)
        fragment.refreshNominatedMedia(0)
        verify(childFragmentManager, times(2)).beginTransaction()
        verify(childFragmentTransaction).remove(mediaDetails)
        verify(childFragmentTransaction, times(2)).commit()
        verify(childFragmentManager, times(2)).executePendingTransactions()
        verify(container).visibility = View.VISIBLE
        verify(tabLayout).visibility = View.GONE
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
    fun `testBackPressed_Case null != mediaDetails && mediaDetails_isVisible_backButtonClicked`() {
        `when`(mediaDetails.isVisible).thenReturn(true)
        Assert.assertEquals(fragment.backPressed(), true)
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed_Case null != mediaDetails && mediaDetails_isVisible_backButtonNotClicked`() {
        `when`(mediaDetails.isVisible).thenReturn(true)
        Assert.assertEquals(fragment.backPressed(), true)
    }

    @Test
    @Throws(Exception::class)
    fun `testBackPressed_Case null != mediaDetails && mediaDetails_isNotVisible`() {
        Assert.assertEquals(fragment.backPressed(), false)
    }

}