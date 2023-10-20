package fr.free.nrw.commons.delete

import android.app.AlertDialog
import android.content.Context
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.contributions.ContributionsListFragment
import fr.free.nrw.commons.review.ReviewController
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
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
    internal  lateinit var pageEditClient: PageEditClient

    @Mock
    internal  lateinit var context: Context

    @Mock
    internal  lateinit var media: Media

    lateinit var deleteHelper: DeleteHelper

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
        whenever(pageEditClient.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        whenever(pageEditClient.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        whenever(pageEditClient.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))

        whenever(media.displayTitle).thenReturn("Test file")

        val creatorName = "Creator"
        whenever(media.author).thenReturn("$creatorName")
        whenever(media.filename).thenReturn("Test file.jpg")
        val makeDeletion = deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
        verify(pageEditClient).appendEdit(eq("User_Talk:$creatorName"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }

    /**
     * Test a failed deletion
     */
    @Test(expected = RuntimeException::class)
    fun makeDeletionForPrependEditFailure() {
        whenever(pageEditClient.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(false))
        whenever(pageEditClient.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        whenever(pageEditClient.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
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
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback);
    }

    @Test
    fun alertDialogPositiveButtonDisableTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback);
        assertEquals(false, deleteHelper.dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
    }

        @Test
    fun alertDialogPositiveButtonEnableTest() {
        val mContext = RuntimeEnvironment.getApplication().applicationContext
        deleteHelper.askReasonAndExecute(media, mContext, "My Question", ReviewController.DeleteReason.COPYRIGHT_VIOLATION, callback);
        deleteHelper.listener.onClick(deleteHelper.dialog,1,true);
        assertEquals(true, deleteHelper.dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled)
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
