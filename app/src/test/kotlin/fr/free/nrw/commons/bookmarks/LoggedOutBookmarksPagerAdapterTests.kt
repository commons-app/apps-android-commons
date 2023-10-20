package fr.free.nrw.commons.bookmarks

import android.content.Context
import androidx.fragment.app.FragmentManager
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * BookmarksPagerAdapter when user is not loggedIn.
 */
class LoggedOutBookmarksPagerAdapterTests {
    @Mock
    private lateinit var bookmarksPagerAdapter: BookmarksPagerAdapter

    @Mock
    private lateinit var fragmentManager: FragmentManager

    @Mock
    private lateinit var context: Context

    /**
     * Setup the adapter
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        bookmarksPagerAdapter = BookmarksPagerAdapter(fragmentManager, context, true)
    }

    /**
     * checkNotNull
     */
    @Test
    fun checkNotNull() {
        Assert.assertNotNull(bookmarksPagerAdapter)
    }

    /**
     * getItems
     * Logged out bookmark adapter has just one item.
     */
    @Test
    fun testGetItem() {
        bookmarksPagerAdapter.getItem(0)
    }

    /**
     * itemCount
     * Logged out bookmark adapter has just one item.
     */
    @Test
    fun testGetCount() {
        Assert.assertEquals(bookmarksPagerAdapter.count, 1)
    }

    /**
     * getTitle.
     */
    @Test
    fun testGetPageTitle() {
        bookmarksPagerAdapter.getPageTitle(0)
    }
}