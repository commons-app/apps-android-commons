package fr.free.nrw.commons.explore.depictions

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.ActivityWikidataItemDetailsBinding
import fr.free.nrw.commons.explore.depictions.media.DepictedImagesFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WikidataItemDetailsActivityUnitTests {

    private lateinit var activity: WikidataItemDetailsActivity

    @Mock
    private lateinit var mediaDetailPagerFragment: MediaDetailPagerFragment

    @Mock
    private lateinit var depictionImagesListFragment: DepictedImagesFragment

    @Mock
    private lateinit var supportFragmentManager: FragmentManager

    @Mock
    private lateinit var depictedItem: DepictedItem

    @Mock
    private lateinit var wikidataItem: DepictedItem


    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            WikidataItemDetailsActivity::class.java
        )
        intent.putExtra("wikidataItemName", "depictionName")
        intent.putExtra("entityId", 0)
        activity =
            Robolectric.buildActivity(WikidataItemDetailsActivity::class.java, intent).create()
                .get()
        Whitebox.setInternalState(activity, "mediaDetailPagerFragment", mediaDetailPagerFragment)
        Whitebox.setInternalState(
            activity,
            "depictionImagesListFragment",
            depictionImagesListFragment
        )
        Whitebox.setInternalState(activity, "supportFragmentManager", supportFragmentManager)


        Whitebox.setInternalState(activity, "wikidataItem", wikidataItem)


    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        activity.viewPagerNotifyDataSetChanged()
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPosition() {
        activity.getMediaAtPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        `when`(supportFragmentManager.backStackEntryCount).thenReturn(1)
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseReturn() {
        `when`(supportFragmentManager.backStackEntryCount).thenReturn(1)
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        activity.totalMediaCount
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        Assert.assertEquals(activity.getContributionStateAt(0), null)
    }

    @Test
    @Throws(Exception::class)
    fun testRefreshNominatedMedia() {
        activity.refreshNominatedMedia(0)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        Assert.assertEquals(activity.onCreateOptionsMenu(RoboMenu()), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseOne() {
        Assert.assertEquals(
            activity.onOptionsItemSelected(RoboMenuItem(R.id.browser_actions_menu_items)),
            true
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseTwo() {
        Assert.assertEquals(activity.onOptionsItemSelected(RoboMenuItem(android.R.id.home)), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        Assert.assertEquals(activity.onOptionsItemSelected(RoboMenuItem()), false)
    }

    @Test
    @Throws(Exception::class)
    fun testStartYourself() {
        WikidataItemDetailsActivity.startYourself(activity, depictedItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaClicked() {
        activity.onMediaClicked(0)
    }

}
