package fr.free.nrw.commons.bookmarks

import android.content.Context
import androidx.fragment.app.FragmentManager
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarksPagerAdapterTests {
    @Mock
    private lateinit var bookmarksPagerAdapter: BookmarksPagerAdapter

    @Mock
    private lateinit var fragmentManager: FragmentManager

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        bookmarksPagerAdapter = BookmarksPagerAdapter(fragmentManager, context, false)
    }

    @Test
    fun checkNotNull() {
        Assert.assertNotNull(bookmarksPagerAdapter)
    }

    @Test
    fun testGetItem() {
        bookmarksPagerAdapter.getItem(0)
        bookmarksPagerAdapter.getItem(1)
    }

    @Test
    fun testGetCount() {
        Assert.assertEquals(bookmarksPagerAdapter.count, 4)
    }

    @Test
    fun testGetPageTitle() {
        bookmarksPagerAdapter.getPageTitle(0)
    }
}
