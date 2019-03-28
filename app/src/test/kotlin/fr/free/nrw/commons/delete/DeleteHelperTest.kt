package fr.free.nrw.commons.delete

import android.accounts.Account
import android.content.Context
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtilWrapper
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Tests for delete helper
 */
class DeleteHelperTest {

    @Mock
    internal var mwApi: MediaWikiApi? = null

    @Mock
    internal var sessionManager: SessionManager? = null

    @Mock
    internal var notificationHelper: NotificationHelper? = null

    @Mock
    internal var context: Context? = null

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
        `when`(mwApi?.editToken).thenReturn("token")
        `when`(sessionManager?.authCookie).thenReturn("Mock cookie")
        `when`(sessionManager?.currentAccount).thenReturn(Account("TestUser", "Test"))
        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
        assertNotNull(makeDeletion)
        assertTrue(makeDeletion!!)
    }

    /**
     * Test a failed deletion
     */
    @Test
    fun makeDeletionForNullToken() {
        `when`(mwApi?.editToken).thenReturn(null)
        `when`(sessionManager?.authCookie).thenReturn("Mock cookie")
        `when`(sessionManager?.currentAccount).thenReturn(Account("TestUser", "Test"))
        `when`(media?.displayTitle).thenReturn("Test file")
        `when`(media?.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper?.makeDeletion(context, media, "Test reason")?.blockingGet()
        assertNotNull(makeDeletion)
        assertFalse(makeDeletion!!)
    }
}