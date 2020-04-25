package fr.free.nrw.commons.delete

import android.content.Context
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.actions.PageEditClient
import io.reactivex.Observable
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * Tests for delete helper
 */
class DeleteHelperTest {

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
        MockitoAnnotations.initMocks(this)
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

        val authorName = "Author"
        whenever(media.author).thenReturn("$authorName")
        whenever(media.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
        verify(pageEditClient).appendEdit(eq("User_Talk:$authorName"), ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
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
        whenever(media.author).thenReturn("Author")

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
        whenever(media.author).thenReturn("Author")

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
        whenever(media.author).thenReturn("Author")

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
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
        media.filename ="Test file.jpg"

        whenever(media.author).thenReturn(null)

        deleteHelper.makeDeletion(context, media, "Test reason")?.blockingGet()
    }
}
