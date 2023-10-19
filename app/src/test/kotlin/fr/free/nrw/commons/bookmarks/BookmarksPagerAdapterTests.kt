package fr.free.nrw.commons.bookmarks

import android.content.Context
import androidx.fragment.app.FragmentManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

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
        Assert.assertEquals(bookmarksPagerAdapter.count, 3)
    }

    @Test
    fun testGetPageTitle() {
        bookmarksPagerAdapter.getPageTitle(0)
    }
}