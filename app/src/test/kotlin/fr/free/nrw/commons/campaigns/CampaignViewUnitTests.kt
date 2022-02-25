package fr.free.nrw.commons.campaigns

import android.app.Activity
import android.view.View
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.data.models.Campaign
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CampaignViewUnitTests {

    private lateinit var activityController: ActivityController<Activity>
    private lateinit var activity: MainActivity
    private lateinit var campaignView: CampaignView
    private lateinit var campaign: Campaign

    @Mock
    private lateinit var view: View

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        AppAdapter.set(TestAppAdapter())

        activityController = Robolectric.buildActivity(Activity::class.java)
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        campaignView = CampaignView(activity)
        campaignView = CampaignView(activity, null)
        campaignView = CampaignView(activity, null, 0)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(campaignView)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCampaignNonNullNonException() {
        campaign = Campaign("", "", "2000-01-01", "2000-01-02", "")
        campaignView.setCampaign(campaign)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCampaignNonNullException() {
        campaign = Campaign("", "", "", "", "")
        campaignView.setCampaign(campaign)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCampaignNull() {
        campaignView.setCampaign(null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSwipe() {
        campaignView.onSwipe(view)
    }

    @Test
    @Throws(Exception::class)
    fun testInit() {
        val method: Method = CampaignView::class.java.getDeclaredMethod(
            "init"
        )
        method.isAccessible = true
        method.invoke(campaignView)
    }

}