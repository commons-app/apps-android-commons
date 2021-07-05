package fr.free.nrw.commons.profile.achievements

import android.content.Context
import android.os.Looper
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.dinuscxj.progressbar.CircleProgressBar
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class AchievementsFragmentUnitTests {

    private lateinit var fragment: AchievementsFragment

    private lateinit var context: Context

    private lateinit var achievements: Achievements

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var badgeText: TextView

    @Mock
    private lateinit var levelNumber: TextView

    @Mock
    private lateinit var thanksReceived: TextView

    @Mock
    private lateinit var imagesUploadedProgressbar: CircleProgressBar

    @Mock
    private lateinit var imagesUsedByWikiProgressBar: CircleProgressBar

    @Mock
    private lateinit var imageRevertsProgressbar: CircleProgressBar

    @Mock
    private lateinit var imagesFeatured: TextView

    @Mock
    private lateinit var tvQualityImages: TextView

    @Mock
    private lateinit var imagesRevertLimitText: TextView

    @Mock
    private lateinit var imageByWikiText: TextView

    @Mock
    private lateinit var imageRevertedText: TextView

    @Mock
    private lateinit var imageUploadedText: TextView

    @Mock
    private lateinit var progressBar: ProgressBar

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        AppAdapter.set(TestAppAdapter())
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = AchievementsFragment()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        achievements = Achievements(0, 0, 0, 0, 0, 0, 0)

        Whitebox.setInternalState(fragment, "thanksReceived", thanksReceived)
        Whitebox.setInternalState(
            fragment,
            "imagesUsedByWikiProgressBar",
            imagesUsedByWikiProgressBar
        )
        Whitebox.setInternalState(
            fragment,
            "imagesUsedByWikiProgressBar",
            imagesUsedByWikiProgressBar
        )
        Whitebox.setInternalState(fragment, "imagesFeatured", imagesFeatured)
        Whitebox.setInternalState(fragment, "tvQualityImages", tvQualityImages)
        Whitebox.setInternalState(fragment, "levelNumber", levelNumber)
        Whitebox.setInternalState(fragment, "imageView", imageView)
        Whitebox.setInternalState(fragment, "badgeText", badgeText)
        Whitebox.setInternalState(fragment, "imagesUploadedProgressbar", imagesUploadedProgressbar)
        Whitebox.setInternalState(fragment, "imageRevertsProgressbar", imageRevertsProgressbar)
        Whitebox.setInternalState(
            fragment,
            "imagesUsedByWikiProgressBar",
            imagesUsedByWikiProgressBar
        )
        Whitebox.setInternalState(fragment, "imageView", imageView)
        Whitebox.setInternalState(fragment, "imageByWikiText", imageByWikiText)
        Whitebox.setInternalState(fragment, "imageRevertedText", imageRevertedText)
        Whitebox.setInternalState(fragment, "imageUploadedText", imageUploadedText)
        Whitebox.setInternalState(fragment, "imageView", imageView)
        Whitebox.setInternalState(fragment, "progressBar", progressBar)
        Whitebox.setInternalState(fragment, "imagesRevertLimitText", imagesRevertLimitText)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowInfoDialog() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showInfoDialog()
    }

    @Test
    @Throws(Exception::class)
    fun testShowUploadInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showUploadInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowRevertedInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showRevertedInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowUsedByWikiInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showUsedByWikiInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowImagesViaNearbyInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showImagesViaNearbyInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowFeaturedImagesInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showFeaturedImagesInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowThanksReceivedInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showThanksReceivedInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowQualityImagesInfo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showQualityImagesInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchAlert() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "launchAlert",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "", "")
    }

    @Test
    @Throws(Exception::class)
    fun testHideProgressBar() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "hideProgressBar",
            Achievements::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, achievements)
    }

    @Test
    @Throws(Exception::class)
    fun testSetAchievementsUploadCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "setAchievementsUploadCount",
            Achievements::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, achievements, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckAccount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "checkAccount"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUploadCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "setUploadCount",
            Achievements::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, achievements)
    }

    @Test
    @Throws(Exception::class)
    fun testOnError() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "onError"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowSnackBarWithRetryTrue() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "showSnackBarWithRetry", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowSnackBarWithRetryFalse() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "showSnackBarWithRetry", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, false)
    }

    @Test
    @Throws(Exception::class)
    fun testSetWikidataEditCount() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "setWikidataEditCount"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testSetAchievements() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = AchievementsFragment::class.java.getDeclaredMethod(
            "setAchievements"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }
}