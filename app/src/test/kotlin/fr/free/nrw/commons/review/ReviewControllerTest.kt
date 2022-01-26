package fr.free.nrw.commons.review

import android.app.Activity
import android.content.Context
import android.os.Looper
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.delete.DeleteHelper
import media
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import org.wikipedia.dataclient.mwapi.MwQueryPage
import java.lang.reflect.Method
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ReviewControllerTest {

    private lateinit var controller: ReviewController
    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var media: Media

    @InjectMocks
    private lateinit var deleteHelper: DeleteHelper

    @Mock
    private lateinit var reviewCallback: ReviewController.ReviewCallback

    @Mock
    private lateinit var firstRevision: MwQueryPage.Revision

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        AppAdapter.set(TestAppAdapter())
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        activity = Robolectric.buildActivity(ReviewActivity::class.java).create().get()
        controller = ReviewController(deleteHelper, context)
        media = media(filename = "test_file", dateUploaded = Date())
        Whitebox.setInternalState(controller, "media", media)
    }

    @Test
    fun testGetMedia() {
        controller.onImageRefreshed(media)
        controller.media
    }

    @Test
    fun testReportSpam() {
        shadowOf(Looper.getMainLooper()).idle()
        controller.reportSpam(activity, reviewCallback)
    }

    @Test
    fun testReportPossibleCopyRightViolation() {
        shadowOf(Looper.getMainLooper()).idle()
        controller.reportPossibleCopyRightViolation(activity, reviewCallback)
    }

    @Test
    fun testReportWrongCategory() {
        shadowOf(Looper.getMainLooper()).idle()
        controller.reportWrongCategory(activity, reviewCallback)
    }

    @Test
    fun testPublishProgress() {
        shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ReviewController::class.java.getDeclaredMethod(
            "publishProgress", Context::class.java, Int::class.java
        )
        method.isAccessible = true
        method.invoke(controller, context, 1)
    }


    @Test
    fun testShowNotification() {
        shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ReviewController::class.java.getDeclaredMethod(
            "showNotification", String::class.java, String::class.java
        )
        method.isAccessible = true
        method.invoke(controller, "", "")
    }

    @Test
    fun testSendThanks() {
        shadowOf(Looper.getMainLooper()).idle()
        whenever(firstRevision.revisionId).thenReturn(1)
        Whitebox.setInternalState(controller, "firstRevision", firstRevision)
        controller.sendThanks(activity)
    }

    @Test
    fun testSendThanksCaseNull() {
        shadowOf(Looper.getMainLooper()).idle()
        controller.sendThanks(activity)
    }
}