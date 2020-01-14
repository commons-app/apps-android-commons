package fr.free.nrw.commons.delete

import android.content.Context
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.wikipedia.AppAdapter
import javax.inject.Inject
import javax.inject.Named

/**
 * Tests for delete helper
 */
class DeleteHelperTest {

    @Mock
    @field:[Inject Named("commons-page-edit")]
    internal var pageEditClient: PageEditClient? = null

    @Mock
    internal var context: Context? = null

    @Mock
    internal var notificationHelper: NotificationHelper? = null

    @Mock
    internal var viewUtil: ViewUtilWrapper? = null

    @Mock
    internal var media: Media? = null

    @InjectMocks
    var deleteHelper: DeleteHelper? = null

    /**
     * Init mocks for test
     */
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    /**
     * Make a successful deletion
     */
    @Test
    fun makeDeletion() {
        `when`(pageEditClient?.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))

        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
    }

    /**
     * Test a failed deletion
     */
    @Test(expected = RuntimeException::class)
    fun makeDeletionForPrependEditFailure() {
        `when`(pageEditClient?.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(false))
        `when`(pageEditClient?.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    @Test(expected = RuntimeException::class)
    fun makeDeletionForEditFailure() {
        `when`(pageEditClient?.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(false))
        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
    }

    @Test(expected = RuntimeException::class)
    fun makeDeletionForAppendEditFailure() {
        `when`(pageEditClient?.prependEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(pageEditClient?.appendEdit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(false))
        `when`(pageEditClient?.edit(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(true))
        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
    }
}