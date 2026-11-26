package fr.free.nrw.commons.customselector.ui.selector

import android.app.AlertDialog
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDialog

/**
 * Custom Selector Dialogs Test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CustomSelectorDialogsTest {
    private lateinit var activity: CustomSelectorActivity

    /**
     * Set up the tests, the activity is only used as the context of the dialogs.
     * It is not created, so no other task is queued on the main looper.
     */
    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).get()
    }

    /**
     * Test the welcome dialog is shown and can be dismissed.
     */
    @Test
    fun testShowWelcomeDialog() {
        CustomSelectorDialogs.showWelcomeDialog(activity)

        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)

        dialog.findViewById<Button>(R.id.btn_ok).performClick()
        assertFalse(dialog.isShowing)
    }

    /**
     * Test the upload limit warning tells the limit and by how much it is exceeded.
     */
    @Test
    fun testShowUploadLimitWarning() {
        CustomSelectorDialogs.showUploadLimitWarning(activity, 5, 2)

        val dialog = ShadowDialog.getLatestDialog()
        assertTrue(dialog.isShowing)
        assertEquals(
            activity.resources.getString(R.string.custom_selector_over_limit_warning, 5, 2),
            dialog.findViewById<TextView>(R.id.upload_limit_warning).text.toString(),
        )

        dialog.findViewById<Button>(R.id.btn_dismiss_limit_warning).performClick()
        assertFalse(dialog.isShowing)
    }

    /**
     * Test the duplicate selection warning continues only when the user confirms.
     */
    @Test
    fun testShowDuplicateSelectionWarning() {
        var confirmed = false

        CustomSelectorDialogs.showDuplicateSelectionWarning(activity) { confirmed = true }

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(dialog.isShowing)
        assertEquals(
            activity.getString(R.string.custom_selector_duplicates_warning),
            shadowOf(dialog).message.toString(),
        )
        assertFalse(confirmed)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(confirmed)
    }
}
