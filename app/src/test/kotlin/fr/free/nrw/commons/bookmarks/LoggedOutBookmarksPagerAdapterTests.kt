package fr.free.nrw.commons.bookmarks

import android.content.Context
import androidx.fragment.app.FragmentManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.equalTo

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
        assertThat(bookmarksPagerAdapter, notNullValue())
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
        assertThat(bookmarksPagerAdapter.count, equalTo( 1))
    }

    /**
     * getTitle.
     */
    @Test
    fun testGetPageTitle() {
        bookmarksPagerAdapter.getPageTitle(0)
    }
}