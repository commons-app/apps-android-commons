package fr.free.nrw.commons.contributions

import android.view.View
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class MainNavigationHelperTest {
    private lateinit var activity: MainActivity
    private lateinit var tabLayout: NavTabLayout
    private lateinit var kvStore: JsonKvStore
    private lateinit var tabHandler: MainTabHandler
    private lateinit var navigationHelper: MainNavigationHelper

    @Before
    fun setUp() {
        OkHttpConnectionFactory.CLIENT = createTestClient()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        tabLayout = mock()
        kvStore = mock()
        activity.applicationKvStore = kvStore
        tabHandler = MainTabHandler(activity, tabLayout)
        navigationHelper = MainNavigationHelper(activity, tabHandler)
    }

    @Test
    fun testFreeUpFragments() {
        navigationHelper.nearbyParentFragment = mock()
        navigationHelper.exploreFragment = mock()
        navigationHelper.bookmarkFragment = mock()

        navigationHelper.freeUpFragments()

        assertNull(navigationHelper.nearbyParentFragment)
        assertNull(navigationHelper.exploreFragment)
        assertNull(navigationHelper.bookmarkFragment)
    }

    @Test
    fun testOnSupportNavigateUpContributionsHandled() {
        val contributionsFragment: ContributionsFragment = mock()
        whenever(contributionsFragment.backButtonClicked()).thenReturn(true)
        navigationHelper.contributionsFragment = contributionsFragment
        navigationHelper.activeFragment = MainActivity.ActiveFragment.CONTRIBUTIONS

        assertTrue(navigationHelper.onSupportNavigateUp())
        verify(contributionsFragment).backButtonClicked()
    }

    @Test
    fun testOnSupportNavigateUpOtherFragments() {
        navigationHelper.activeFragment = MainActivity.ActiveFragment.NEARBY
        assertTrue(navigationHelper.onSupportNavigateUp())
        verify(tabLayout).visibility = View.VISIBLE
    }

    @Test
    fun testRestoreActiveFragmentContributions() {
        navigationHelper.restoreActiveFragment(MainActivity.ActiveFragment.CONTRIBUTIONS.name)
        assertEquals(activity.getString(R.string.contributions_fragment), activity.title)
    }

    @Test
    fun testRestoreActiveFragmentNearby() {
        navigationHelper.restoreActiveFragment(MainActivity.ActiveFragment.NEARBY.name)
        assertEquals(activity.getString(R.string.nearby_fragment), activity.title)
    }

    @Test
    fun testRestoreActiveFragmentExplore() {
        navigationHelper.restoreActiveFragment(MainActivity.ActiveFragment.EXPLORE.name)
        assertEquals(activity.getString(R.string.navigation_item_explore), activity.title)
    }

    @Test
    fun testRestoreActiveFragmentBookmark() {
        navigationHelper.restoreActiveFragment(MainActivity.ActiveFragment.BOOKMARK.name)
        assertEquals(activity.getString(R.string.bookmarks), activity.title)
    }

    @Test
    fun testCenterMapToPlace() {
        val place: Place = mock()
        val nearbyFragment: NearbyParentFragment = mock()
        navigationHelper.nearbyParentFragment = nearbyFragment

        navigationHelper.centerMapToPlace(place)
        verify(tabLayout).selectedItemId = NavTab.NEARBY.code()
        verify(nearbyFragment).setNearbyParentFragmentInstanceReadyCallback(any())
    }

    @Test
    fun testLoadExploreMapFromNearby() {
        navigationHelper.loadExploreMapFromNearby(10.0, 20.0, 30.0)
        verify(tabLayout).selectedItemId = NavTab.EXPLORE.code()
    }

    @Test
    fun testLoadNearbyMapFromExplore() {
        navigationHelper.loadNearbyMapFromExplore(12.0, 22.0, 32.0)
        verify(tabLayout).selectedItemId = NavTab.NEARBY.code()
    }

    @Test
    fun testSetupBackNavigationCallbackRegistration() {
        val callback = navigationHelper.setupBackNavigation()
        assertTrue(callback.isEnabled)
    }
}
