package fr.free.nrw.commons.delete

import android.app.AlertDialog
import android.content.Context
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.review.ReviewController
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import fr.free.nrw.commons.R
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.spy
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Tests for delete helper
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DeleteHelperTest {
    @Mock
    private lateinit var callback: ReviewController.ReviewCallback

    @Mock
    internal lateinit var pageEditClient: PageEditClient

    @Mock
    internal lateinit var context: Context

    @Mock
    internal lateinit var media: Media

    private lateinit var deleteHelper: DeleteHelper

    /**
     * Init mocks for test
     */
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        deleteHelper = DeleteHelper(mock(), pageEditClient, mock(), "")
    }

    /**
     * Make a successful deletion
     */
    @Test
    fun makeDeletion() {
        whenever(pageEditClient.prependEdit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(true))

        whenever(pageEditClient.appendEdit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(true))

        whenever(pageEditClient.edit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(true))

        whenever(media.displayTitle).thenReturn("Test file")

        `when`(context.getString(R.string.delete_helper_show_deletion_title))
            .thenReturn("Deletion Notification")
        `when`(context.getString(R.string.delete_helper_show_deletion_title_success))
            .thenReturn("Success")
        `when`(context.getString(R.string.delete_helper_show_deletion_title_failed))
            .thenReturn("Failed")
        `when`(context.getString(R.string.delete_helper_show_deletion_message_else))
            .thenReturn("Media deletion failed")
        `when`(context.getString(
            R.string.delete_helper_show_deletion_message_if, media.displayTitle)
        ).thenReturn("Media successfully deleted: Test Media Title")

        val creatorName = "Creator"
        whenever(media.author).thenReturn("$creatorName")
        whenever(media.filename).thenReturn("Test file.jpg")
        val makeDeletion = deleteHelper.makeDeletion(
            context,
            media,
            "Test reason"
        )?.blockingGet()
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
        verify(pageEditClient).appendEdit(eq("User_Talk:$creatorName"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

    /**
     * Test a failed deletion
     */
    @Test(expected = RuntimeException::class)
    fun makeDeletionForPrependEditFailure() {
        whenever(pageEditClient.prependEdit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(false))

        whenever(pageEditClient.appendEdit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(true))

        whenever(pageEditClient.edit(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())
        ).thenReturn(Observable.just(true))

        whenever(media.displayTitle).thenReturn("Test file")
        whenever(media.filename).thenReturn("Test file.jpg")
        whenever(media.author).thenReturn("Creator (page does not exist)")

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    @Test(expected = RuntimeException::class)
    fun makeDeletionForEditFailure() {
        whenever(pageEditClient.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(pageEditClient.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(pageEditClient.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(false))
        whenever(media.displayTitle).thenReturn("Test file")
        whenever(media.filename).thenReturn("Test file.jpg")
        whenever(media.author).thenReturn("Creator (page does not exist)")

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    @Test(expected = RuntimeException::class)
    fun makeDeletionForAppendEditFailure() {
        whenever(pageEditClient.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(pageEditClient.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(false))
        whenever(pageEditClient.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(media.displayTitle).thenReturn("Test file")
        whenever(media.filename).thenReturn("Test file.jpg")
        whenever(media.author).thenReturn("Creator (page does not exist)")

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    @Test
    fun askReasonAndExecuteSpamTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.SPAM, callback)
    }

    @Test
    fun askReasonAndExecuteCopyrightViolationTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback)
    }

    @Test
    fun alertDialogPositiveButtonDisableTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(
            media,
            mContext,
            "My Question",
            ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback
        )

        deleteHelper.getListener()?.onClick(
            deleteHelper.getDialog(),
            1,
            true
        )
        assertEquals(
            true,
            deleteHelper.getDialog()?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled
        )
    }

    @Test
    fun alertDialogPositiveButtonEnableTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback)
        deleteHelper.getListener()?.onClick(deleteHelper.getDialog(), 1, true)
        assertEquals(true, deleteHelper.getDialog()?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled)
    }

    @Test(expected = RuntimeException::class)
    fun makeDeletionForEmptyCreatorName() {
        whenever(pageEditClient.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(pageEditClient.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))
        whenever(pageEditClient.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
            .thenReturn(Observable.just(true))

        whenever(media.displayTitle).thenReturn("Test file")
        whenever(media.filename).thenReturn("Test file.jpg")

        whenever(media.author).thenReturn(null)

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }
}
