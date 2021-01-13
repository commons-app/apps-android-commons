package fr.free.nrw.commons.contributions

import android.content.Context
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.navtab.NavTabLayout
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MainActivityUnitTests {

    private lateinit var activity: MainActivity

    private lateinit var context: Context

    private lateinit var menuItem: RoboMenuItem

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var tabLayout: NavTabLayout

    @Mock
    private lateinit var nearbyParentFragment: NearbyParentFragment


    @Before
    fun setUp() {

        MockitoAnnotations.initMocks(this)

        activity = Robolectric.buildActivity(MainActivity::class.java).get()

        context = RuntimeEnvironment.application.applicationContext

        menuItem = RoboMenuItem(context)

        val fieldNavTabLayout: Field =
            MainActivity::class.java.getDeclaredField("tabLayout")
        fieldNavTabLayout.isAccessible = true
        fieldNavTabLayout.set(activity, tabLayout)

        val fieldNearbyParentFragment: Field =
            MainActivity::class.java.getDeclaredField("nearbyParentFragment")
        fieldNearbyParentFragment.isAccessible = true
        fieldNearbyParentFragment.set(activity, nearbyParentFragment)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
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
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackStackChanged() {
        activity.onBackStackChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
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

}