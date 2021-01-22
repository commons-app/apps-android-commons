package fr.free.nrw.commons.explore.search

import android.content.Context
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.explore.SearchActivity
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
import org.robolectric.annotation.LooperMode


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SearchActivityUnitTests {

    @Mock
    private lateinit var activity: SearchActivity

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        activity = Robolectric.buildActivity(SearchActivity::class.java).create().get()

        context = RuntimeEnvironment.application.applicationContext
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetTabs() {
        activity.setTabs()
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateText() {
        activity.updateText("test")
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        activity.viewPagerNotifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        activity.getContributionStateAt(0)
    }

}