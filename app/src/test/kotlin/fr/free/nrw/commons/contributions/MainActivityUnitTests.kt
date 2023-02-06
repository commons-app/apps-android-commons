package fr.free.nrw.commons.contributions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter
import java.lang.reflect.Field
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MainActivityUnitTests {

    private lateinit var activity: MainActivity
    private lateinit var context: Context
    private lateinit var menuItem: RoboMenuItem
    private lateinit var mainActivity: MainActivity
    private lateinit var mockContext: Context

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var tabLayout: NavTabLayout

    @Mock
    private lateinit var nearbyParentFragment: NearbyParentFragment

    @Mock
    private lateinit var contributionsFragment: ContributionsFragment

    @Mock
    private lateinit var activeFragment: ActiveFragment

    @Mock
    private lateinit var bookmarkFrament: BookmarkFragment

    @Mock
    private lateinit var exploreFragment: ExploreFragment

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var bundle: Bundle

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        activity.applicationKvStore = applicationKvStore
        context = ApplicationProvider.getApplicationContext()
        menuItem = RoboMenuItem(context)
        mockContext = PowerMockito.mock(Context::class.java)
        mainActivity = PowerMockito.mock(MainActivity::class.java)

        val fieldNavTabLayout: Field =
            MainActivity::class.java.getDeclaredField("tabLayout")
        fieldNavTabLayout.isAccessible = true
        fieldNavTabLayout.set(activity, tabLayout)

        val fieldNearbyParentFragment: Field =
            MainActivity::class.java.getDeclaredField("nearbyParentFragment")
        fieldNearbyParentFragment.isAccessible = true
        fieldNearbyParentFragment.set(activity, nearbyParentFragment)

        val fieldContributionsFragment: Field =
            MainActivity::class.java.getDeclaredField("contributionsFragment")
        fieldContributionsFragment.isAccessible = true
        fieldContributionsFragment.set(activity, contributionsFragment)

        val config: Configuration = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUpCaseDefault() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        activity.onSupportNavigateUp()
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        activity.activeFragment = activeFragment
        activity.onSupportNavigateUp()
    }

    @Test
    @Throws(Exception::class)
    fun testCenterMapToPlace() {
        activity.centerMapToPlace(place)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        menuItem = RoboMenuItem(R.id.notifications)
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseDefault() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackStackChanged() {
        activity.onBackStackChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseDefault() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testSetNumOfUploads() {
        activity.setNumOfUploads(0)
    }

    @Test
    @Throws(Exception::class)
    fun testShowTabs() {
        activity.showTabs()
    }

    @Test
    @Throws(Exception::class)
    fun testHideTabs() {
        activity.hideTabs()
    }

    @Test
    @Throws(Exception::class)
    fun testSetSelectedItemId() {
        activity.setSelectedItemId(0)
    }

    @Test
    @Throws(Exception::class)
    fun testStartYourself() {
        MainActivity.startYourself(mockContext)
    }

    @Test
    @Throws(Exception::class)
    fun testToggleLimitedConnectionModeCaseDefault() {
        activity.toggleLimitedConnectionMode()
    }

    @Test
    @Throws(Exception::class)
    fun testToggleLimitedConnectionMode() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(
            defaultKvStore.getBoolean(
                CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false
            )
        )
            .thenReturn(false)
        activity.toggleLimitedConnectionMode()
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpPager() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "setUpPager"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpLoggedOutPager() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "setUpLoggedOutPager"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseContributionsFragment() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, contributionsFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseContributionsFragmentCaseTrue() {
        activeFragment = ActiveFragment.CONTRIBUTIONS
        activity.activeFragment = activeFragment
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, contributionsFragment, false)
        verify(contributionsFragment).scrollToTop();
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNearbyParentFragmentCaseTrue() {
        activeFragment = ActiveFragment.NEARBY
        activity.activeFragment = activeFragment
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, nearbyParentFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNearbyParentFragment() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, nearbyParentFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseExploreFragmentCaseTrue() {
        activeFragment = ActiveFragment.EXPLORE
        activity.activeFragment = activeFragment
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, exploreFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseExploreFragment() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, exploreFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseBookmarkFragmentCaseTrue() {
        activeFragment = ActiveFragment.BOOKMARK
        activity.activeFragment = activeFragment
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bookmarkFrament, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseBookmarkFragment() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bookmarkFrament, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNull() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "loadFragment",
            Fragment::class.java,
            Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResult() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onActivityResult",
            Int::class.java,
            Int::class.java,
            Intent::class.java
        )
        method.isAccessible = true
        method.invoke(activity, 0, 0, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onResume"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onDestroy"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPostCreate() {
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onPostCreate",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onSaveInstanceState",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseContributions() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.CONTRIBUTIONS.name)
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onRestoreInstanceState",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseNearby() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.NEARBY.name)
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onRestoreInstanceState",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseExplore() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.EXPLORE.name)
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onRestoreInstanceState",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseBookmark() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.BOOKMARK.name)
        val method: Method = MainActivity::class.java.getDeclaredMethod(
            "onRestoreInstanceState",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSetUpPagerNearBy(){
        val item = Mockito.mock(MenuItem::class.java)
        `when`(item.title).thenReturn(activity.getString(R.string.nearby_fragment))
        activity.navListener.onNavigationItemSelected(item)
        verify(item, Mockito.times(3)).title
        verify(applicationKvStore,Mockito.times(1))
            .putBoolean("last_opened_nearby",true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSetUpPagerOtherThanNearBy(){
        val item = Mockito.mock(MenuItem::class.java)
        `when`(item.title).thenReturn(activity.getString(R.string.bookmarks))
        activity.navListener.onNavigationItemSelected(item)
        verify(item, Mockito.times(3)).title
        verify(applicationKvStore,Mockito.times(1))
            .putBoolean("last_opened_nearby",false)
    }

}