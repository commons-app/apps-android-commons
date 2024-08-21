package fr.free.nrw.commons.recentlanguages

import android.net.Uri
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.data.DBOpenHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class RecentLanguagesContentProviderUnitTest {

    private lateinit var contentProvider: RecentLanguagesContentProvider

    @Mock
    lateinit var dbOpenHelper: DBOpenHelper

    @Before
    fun setUp(){
        MockitoAnnotations.openMocks(this)
        contentProvider = RecentLanguagesContentProvider()
        Whitebox.setInternalState(contentProvider, "dbOpenHelper", dbOpenHelper)
    }

    @Test
    @Throws(Exception::class)
    fun testGetType() {
        contentProvider.getType(mock(Uri::class.java))
    }
}