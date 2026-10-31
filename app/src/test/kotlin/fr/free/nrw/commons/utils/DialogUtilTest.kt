package fr.free.nrw.commons.utils

import android.app.Activity
import android.view.View
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class DialogUtilTest {
    private lateinit var activity: Activity

    @Mock
    private lateinit var mockView: View

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
    }

    @Test
    fun testShowAlertDialogWithOptions() {
        val options =
            DialogOptions(
                title = "Test Title",
                message = "Test Message",
                positiveButtonText = "OK",
                negativeButtonText = "Cancel",
            )
        val dialog = DialogUtil.showAlertDialog(activity, options)
        assertNotNull(dialog)
    }

    @Test
    fun testShowAlertDialogWithCustomViewHasParentReturnsNull() {
        val parent = org.mockito.Mockito.mock(android.view.ViewParent::class.java)
        `when`(mockView.parent).thenReturn(parent)

        val options = DialogOptions(customView = mockView)
        val dialog = DialogUtil.showAlertDialog(activity, options)
        assertNull(dialog)
    }

    @Test
    fun testShowAlertDialogConvenienceOverloads() {
        val dialog1 =
            DialogUtil.showAlertDialog(
                activity = activity,
                title = "Title",
                message = "Message",
                onPositiveBtnClick = {},
                onNegativeBtnClick = {},
            )
        assertNotNull(dialog1)

        val dialog2 =
            DialogUtil.showAlertDialog(
                activity = activity,
                title = "Title",
                message = "Message",
                positiveButtonText = "Yes",
                onPositiveBtnClick = {},
            )
        assertNotNull(dialog2)
    }
}
