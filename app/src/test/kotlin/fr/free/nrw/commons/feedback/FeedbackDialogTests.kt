package fr.free.nrw.commons.feedback

import android.content.Context
import android.os.Looper.getMainLooper
import android.text.Editable
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.doReturn
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.DialogFeedbackBinding
import fr.free.nrw.commons.ui.PasteSensitiveTextInputEditText
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class FeedbackDialogTests {
    private lateinit var dialogFeedbackBinding: DialogFeedbackBinding

    @Mock
    private val onFeedbackSubmitCallback: OnFeedbackSubmitCallback? = null
    private lateinit var dialog: FeedbackDialog

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        dialog = FeedbackDialog(activity.applicationContext, onFeedbackSubmitCallback)
        dialogFeedbackBinding = DialogFeedbackBinding.inflate(dialog.layoutInflater)
        dialog.show()

        Whitebox.setInternalState(dialog, "onFeedbackSubmitCallback", onFeedbackSubmitCallback)
        Whitebox.setInternalState(dialog, "dialogFeedbackBinding", dialogFeedbackBinding)
    }

    @Test
    fun testOnCreate() {
        dialog.onCreate(null)
    }

    @Test
    fun testSubmitFeedbackError() {
        val editable = mock(Editable::class.java)
        val ed = mock(PasteSensitiveTextInputEditText::class.java)
        setFinalStatic(
                DialogFeedbackBinding::class.java.getDeclaredField("feedbackItemEditText"),
                ed)
        `when`(ed?.text).thenReturn(editable)
        doReturn(editable).`when`(ed)?.text
        doReturn("").`when`(editable).toString()
        dialog.submitFeedback()
    }

    @Test
    fun testSubmitFeedback() {
        shadowOf(getMainLooper()).idle()
        val editable: Editable = mock(Editable::class.java)
        val ed = mock(PasteSensitiveTextInputEditText::class.java)
        setFinalStatic(
                DialogFeedbackBinding::class.java.getDeclaredField("feedbackItemEditText"),
                ed)
        `when`(ed?.text).thenReturn(editable)
        `when`(editable.toString()).thenReturn("1234")

        Assert.assertEquals(ed.text.toString(), "1234")
        dialog.submitFeedback()
    }

}