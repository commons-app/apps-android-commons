package fr.free.nrw.commons.contributions

import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.navtab.NavTabLayout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21])
class MainTabHandlerTest {
    private lateinit var activity: MainActivity
    private lateinit var tabLayout: NavTabLayout
    private lateinit var kvStore: JsonKvStore
    private lateinit var tabHandler: MainTabHandler

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(MainActivity::class.java).get()
        tabLayout = mock()
        kvStore = mock()
        activity.applicationKvStore = kvStore
        tabHandler = MainTabHandler(activity, tabLayout)
    }

    @Test
    fun testShowTabs() {
        tabHandler.showTabs()
        verify(tabLayout).visibility = View.VISIBLE
    }

    @Test
    fun testHideTabs() {
        tabHandler.hideTabs()
        verify(tabLayout).visibility = View.GONE
    }

    @Test
    fun testSetSelectedItemId() {
        tabHandler.setSelectedItemId(123)
        verify(tabLayout).selectedItemId = 123
    }

    @Test
    fun testShowNearby() {
        tabHandler.showNearby()
        verify(tabLayout).selectedItemId = NavTab.NEARBY.code()
    }

    @Test
    fun testSetNumOfUploadsZero() {
        tabHandler.setNumOfUploads(0, MainActivity.ActiveFragment.CONTRIBUTIONS)
        val expected =
            activity.resources.getString(R.string.contributions_fragment) + " " +
                activity.getString(R.string.contributions_subtitle_zero)
        assertEquals(expected, activity.title)
    }

    @Test
    fun testSetNumOfUploadsPositive() {
        tabHandler.setNumOfUploads(5, MainActivity.ActiveFragment.CONTRIBUTIONS)
        val expected =
            activity.resources.getString(R.string.contributions_fragment) + " " +
                activity.resources.getQuantityString(R.plurals.contributions_subtitle, 5, 5)
        assertEquals(expected, activity.title)
    }

    @Test
    fun testSetNumOfUploadsOtherFragment() {
        activity.title = "Original Title"
        tabHandler.setNumOfUploads(5, MainActivity.ActiveFragment.NEARBY)
        assertEquals("Original Title", activity.title)
    }

    @Test
    fun testSetUpPager() {
        var callbackInvoked = false
        tabHandler.setUpPager { _: Fragment?, _: Boolean ->
            callbackInvoked = true
            true
        }
        val item: MenuItem = mock()
        whenever(item.title).thenReturn(activity.getString(R.string.nearby_fragment))
        whenever(item.order).thenReturn(NavTab.NEARBY.code())

        tabHandler.navListener?.onNavigationItemSelected(item)
        verify(kvStore).putBoolean("last_opened_nearby", true)
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun testSetUpLoggedOutPager() {
        var callbackInvoked = false
        tabHandler.setUpLoggedOutPager { _: Fragment?, _: Boolean ->
            callbackInvoked = true
            true
        }
    }
}
