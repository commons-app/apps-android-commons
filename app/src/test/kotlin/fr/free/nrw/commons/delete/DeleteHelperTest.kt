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
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import fr.free.nrw.commons.R
import fr.free.nrw.commons.media.MediaClient
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

    @Mock
    internal lateinit var mediaClient: MediaClient

    private lateinit var deleteHelper: DeleteHelper

    /**
     * Init mocks for test
     */
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        deleteHelper = DeleteHelper(mock(), pageEditClient, mock(), mediaClient,"")
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
        whenever(media.getAuthorOrUser()).thenReturn("$creatorName")
        whenever(media.filename).thenReturn("Test file.jpg")
        
        // Mock getImageInfoList to return list with single uploader
        val mockImageInfo = mock<ImageInfo>()
        whenever(mockImageInfo.getUser()).thenReturn(creatorName)
        whenever(mediaClient.getImageInfoList("Test file.jpg"))
            .thenReturn(Single.just(listOf(mockImageInfo)))
        
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
        whenever(media.getAuthorOrUser()).thenReturn("Creator (page does not exist)")
        
        // Mock getImageInfoList
        val mockImageInfo = mock<ImageInfo>()
        whenever(mockImageInfo.getUser()).thenReturn("Creator (page does not exist)")
        whenever(mediaClient.getImageInfoList("Test file.jpg"))
            .thenReturn(Single.just(listOf(mockImageInfo)))

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
        whenever(media.getAuthorOrUser()).thenReturn("Creator (page does not exist)")
        
        // Mock getImageInfoList
        val mockImageInfo = mock<ImageInfo>()
        whenever(mockImageInfo.getUser()).thenReturn("Creator (page does not exist)")
        whenever(mediaClient.getImageInfoList("Test file.jpg"))
            .thenReturn(Single.just(listOf(mockImageInfo)))

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
        whenever(media.getAuthorOrUser()).thenReturn("Creator (page does not exist)")
        
        // Mock getImageInfoList
        val mockImageInfo = mock<ImageInfo>()
        whenever(mockImageInfo.getUser()).thenReturn("Creator (page does not exist)")
        whenever(mediaClient.getImageInfoList("Test file.jpg"))
            .thenReturn(Single.just(listOf(mockImageInfo)))

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

        whenever(media.getAuthorOrUser()).thenReturn(null)

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    /**
     * Test that deletion notification is sent to all uploaders (multiple users)
     * This tests issue #3546 fix
     */
    @Test
    fun makeDeletionNotifiesAllUploaders() {
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
        whenever(media.filename).thenReturn("Test file.jpg")
        whenever(media.getAuthorOrUser()).thenReturn("User1")

        `when`(context.getString(R.string.delete_helper_show_deletion_title))
            .thenReturn("Deletion Notification")
        `when`(context.getString(R.string.delete_helper_show_deletion_title_success))
            .thenReturn("Success")
        `when`(context.getString(
            R.string.delete_helper_show_deletion_message_if, media.displayTitle)
        ).thenReturn("Media successfully deleted")

        // Mock getImageInfoList to return THREE different uploaders
        val mockImageInfo1 = mock<ImageInfo>()
        val mockImageInfo2 = mock<ImageInfo>()
        val mockImageInfo3 = mock<ImageInfo>()
        whenever(mockImageInfo1.getUser()).thenReturn("User1")
        whenever(mockImageInfo2.getUser()).thenReturn("User2")
        whenever(mockImageInfo3.getUser()).thenReturn("User3")
        
        whenever(mediaClient.getImageInfoList("Test file.jpg"))
            .thenReturn(Single.just(listOf(mockImageInfo1, mockImageInfo2, mockImageInfo3)))

        val makeDeletion = deleteHelper.makeDeletion(
            context,
            media,
            "Test reason"
        )?.blockingGet()
        
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
        
        // Verify that ALL three users received notifications on their talk pages
        verify(pageEditClient).appendEdit(eq("User_Talk:User1"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        verify(pageEditClient).appendEdit(eq("User_Talk:User2"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
        verify(pageEditClient).appendEdit(eq("User_Talk:User3"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }
}
