package fr.free.nrw.commons.profile.achievements

import android.content.Context
import android.graphics.Bitmap
import android.view.MenuItem
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
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
import org.wikipedia.AppAdapter


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AchievementsFragmentUnitTests {

    private lateinit var fragment: AchievementsFragment

    private lateinit var context: Context

    private lateinit var menuItem: MenuItem

    @Mock
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        menuItem = RoboMenuItem(context)
        AppAdapter.set(TestAppAdapter())
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = AchievementsFragment()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowAlert() {
        fragment.showAlert(bitmap)
    }


    @Test
    @Throws(Exception::class)
    fun testShowInfoDialog() {
        fragment.showInfoDialog()
    }

    @Test
    @Throws(Exception::class)
    fun testShowUploadInfo() {
        fragment.showUploadInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowRevertedInfo() {
        fragment.showRevertedInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowUsedByWikiInfo() {
        fragment.showUsedByWikiInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowImagesViaNearbyInfo() {
        fragment.showImagesViaNearbyInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowFeaturedImagesInfo() {
        fragment.showFeaturedImagesInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowThanksReceivedInfo() {
        fragment.showThanksReceivedInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowQualityImagesInfo() {
        fragment.showQualityImagesInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        fragment.onOptionsItemSelected(menuItem)
    }

}