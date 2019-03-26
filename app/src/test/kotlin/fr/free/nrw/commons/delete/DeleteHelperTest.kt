package fr.free.nrw.commons.delete

import android.accounts.Account
import android.content.Context
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.MediaWikiApi
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.utils.ViewUtil
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(ViewUtil::class)
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
    internal var media: Media? = null

    @InjectMocks
    var deleteHelper: DeleteHelper? = null

    @Test
    fun makeDeletion() {
        `when`(mwApi!!.editToken).thenReturn("token")
        `when`(sessionManager!!.authCookie).thenReturn("Mock cookie")
        `when`(sessionManager!!.currentAccount).thenReturn(Account("TestUser", "Test"))

        MockitoAnnotations.initMocks(this)
        PowerMockito.mockStatic(ViewUtil::class.java)

        `when`(media!!.displayTitle).thenReturn("Test file")
        `when`(media!!.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper!!.makeDeletion(context, media, "Test reason").blockingGet()
        assertTrue(makeDeletion)
    }

    @Test
    fun makeDeletionForNullToken() {
        `when`(mwApi!!.editToken).thenReturn(null)
        `when`(sessionManager!!.authCookie).thenReturn("Mock cookie")
        `when`(sessionManager!!.currentAccount).thenReturn(Account("TestUser", "Test"))

        MockitoAnnotations.initMocks(this)
        PowerMockito.mockStatic(ViewUtil::class.java)

        `when`(media!!.displayTitle).thenReturn("Test file")
        `when`(media!!.filename).thenReturn("Test file.jpg")

        val makeDeletion = deleteHelper!!.makeDeletion(context, media, "Test reason").blockingGet()
        assertFalse(makeDeletion)
    }
}