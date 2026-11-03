package fr.free.nrw.commons.contributions

import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.BookmarkFragment
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.createTestClient
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
import org.robolectric.annotation.SQLiteMode
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Field
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
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
    private lateinit var bundle: Bundle

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()

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

        activity.navigationHelper.nearbyParentFragment = nearbyParentFragment
        activity.navigationHelper.contributionsFragment = contributionsFragment

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
    fun testOnBackPressedCaseDefault() {
        activity.onBackPressedDispatcher.onBackPressed()
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
    fun testSetUpPager() {
        activity.tabHandler.setUpPager { fragment, showBottom ->
            activity.navigationHelper.loadFragment(fragment, showBottom)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpLoggedOutPager() {
        activity.tabHandler.setUpLoggedOutPager { fragment, showBottom ->
            activity.navigationHelper.loadFragment(fragment, showBottom)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseContributionsFragment() {
        activity.navigationHelper.loadFragment(contributionsFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseContributionsFragmentCaseTrue() {
        activeFragment = ActiveFragment.CONTRIBUTIONS
        activity.activeFragment = activeFragment
        activity.navigationHelper.loadFragment(contributionsFragment, false)
        verify(contributionsFragment).scrollToTop()
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNearbyParentFragmentCaseTrue() {
        activeFragment = ActiveFragment.NEARBY
        activity.activeFragment = activeFragment
        activity.navigationHelper.loadFragment(nearbyParentFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNearbyParentFragment() {
        activity.navigationHelper.loadFragment(nearbyParentFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseExploreFragmentCaseTrue() {
        activeFragment = ActiveFragment.EXPLORE
        activity.activeFragment = activeFragment
        activity.navigationHelper.loadFragment(exploreFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseExploreFragment() {
        activity.navigationHelper.loadFragment(exploreFragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseBookmarkFragmentCaseTrue() {
        activeFragment = ActiveFragment.BOOKMARK
        activity.activeFragment = activeFragment
        activity.navigationHelper.loadFragment(bookmarkFrament, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseBookmarkFragment() {
        activity.navigationHelper.loadFragment(bookmarkFrament, false)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadFragmentCaseNull() {
        activity.navigationHelper.loadFragment(null, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onResume",
            )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onDestroy",
            )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSaveInstanceState() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onSaveInstanceState",
                Bundle::class.java,
            )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseContributions() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.CONTRIBUTIONS.name)
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onRestoreInstanceState",
                Bundle::class.java,
            )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseNearby() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.NEARBY.name)
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onRestoreInstanceState",
                Bundle::class.java,
            )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseExplore() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.EXPLORE.name)
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onRestoreInstanceState",
                Bundle::class.java,
            )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnRestoreInstanceStateCaseBookmark() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(bundle.getString("activeFragment")).thenReturn(ActiveFragment.BOOKMARK.name)
        val method: Method =
            MainActivity::class.java.getDeclaredMethod(
                "onRestoreInstanceState",
                Bundle::class.java,
            )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSetUpPagerNearBy() {
        val item = Mockito.mock(MenuItem::class.java)
        `when`(item.title).thenReturn(activity.getString(R.string.nearby_fragment))
        activity.navListener?.onNavigationItemSelected(item)
        verify(item, Mockito.times(3)).title
        verify(applicationKvStore, Mockito.times(1))
            .putBoolean("last_opened_nearby", true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSetUpPagerOtherThanNearBy() {
        val item = Mockito.mock(MenuItem::class.java)
        `when`(item.title).thenReturn(activity.getString(R.string.bookmarks))
        activity.navListener?.onNavigationItemSelected(item)
        verify(item, Mockito.times(3)).title
        verify(applicationKvStore, Mockito.times(1))
            .putBoolean("last_opened_nearby", false)
    }
}
