package fr.free.nrw.commons.feedback

import android.content.Context
import android.os.Looper.getMainLooper
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import androidx.fragment.app.FragmentTransaction
import com.nhaarman.mockitokotlin2.doReturn
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.SearchActivity
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class FeedbackDialogTests {
    @Mock
    var button: Button? = null
    @Mock
    var apiLevel: CheckBox? = null
    @Mock
    var androidVersion: CheckBox? = null
    @Mock
    var deviceManufacturer: CheckBox? = null
    @Mock
    var deviceModel: CheckBox? = null
    @Mock
    var deviceName: CheckBox? = null
    @Mock
    var networkType: CheckBox? = null
    @Mock
    var userName: CheckBox? = null
    @Mock
    var feedbackDescription: PasteSensitiveTextInputEditText? = null

    @Mock
    private val onFeedbackSubmitCallback: OnFeedbackSubmitCallback? = null
    private lateinit var dialog: FeedbackDialog

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        context = RuntimeEnvironment.application.applicationContext
        AppAdapter.set(TestAppAdapter())

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        dialog = FeedbackDialog(activity.applicationContext, onFeedbackSubmitCallback)
        dialog.show()

        Whitebox.setInternalState(dialog, "button", button)
        Whitebox.setInternalState(dialog, "apiLevel", apiLevel)
        Whitebox.setInternalState(dialog, "androidVersion", androidVersion)
        Whitebox.setInternalState(dialog, "deviceManufacturer", deviceManufacturer)
        Whitebox.setInternalState(dialog, "deviceModel", deviceModel)
        Whitebox.setInternalState(dialog, "deviceName", deviceName)
        Whitebox.setInternalState(dialog, "networkType", networkType)
        Whitebox.setInternalState(dialog, "userName", userName)
        Whitebox.setInternalState(dialog, "feedbackDescription", feedbackDescription)
        Whitebox.setInternalState(dialog, "onFeedbackSubmitCallback", onFeedbackSubmitCallback)
    }

    @Test
    fun testOnCreate() {
        dialog.onCreate(null)
    }

    @Test
    fun testSubmitFeedbackError() {
        doReturn("").`when`(feedbackDescription)?.text.toString()
        dialog.submitFeedback()
    }

    @Test
    fun testSubmitFeedback() {
        shadowOf(getMainLooper()).idle()
        val editable: Editable = mock(Editable::class.java)

        `when`(feedbackDescription?.text).thenReturn(editable)
        `when`(editable.toString()).thenReturn("1234")

        Assert.assertEquals(feedbackDescription?.text.toString(), "1234")
        dialog.submitFeedback()
    }

}