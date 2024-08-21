package fr.free.nrw.commons.quiz

import android.content.Intent
import android.graphics.Bitmap
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class QuizResultActivityUnitTest {

    private lateinit var activity: QuizResultActivity
    private lateinit var quizResultActivity: QuizResultActivity
    private lateinit var menu: RoboMenu

    @Mock
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val intent = Intent().putExtra("QuizResult", 0)
        activity = Robolectric.buildActivity(QuizResultActivity::class.java, intent).get()
        quizResultActivity = PowerMockito.mock(QuizResultActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateCaseDefault() {
        activity.onCreate(null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreate() {
        Mockito.`when`(quizResultActivity.intent).thenReturn(null)
        activity.onCreate(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchContributionActivity() {
        activity.launchContributionActivity()
    }

    @Test
    @Throws(Exception::class)
    fun tesOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun tesOnCreateOptionsMenu() {
        menu = RoboMenu()
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun tesShowAlert() {
        activity.showAlert(bitmap)
    }

    @Test
    @Throws(Exception::class)
    fun tesShareScreen() {
        activity.shareScreen(bitmap)
    }

}