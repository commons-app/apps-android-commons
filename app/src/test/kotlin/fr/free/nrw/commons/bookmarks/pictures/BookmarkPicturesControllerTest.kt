package fr.free.nrw.commons.bookmarks.pictures

import android.net.Uri
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.models.Media
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import media
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.*

/**
 * Tests for bookmark pictures controller
 */
class BookmarkPicturesControllerTest {
    @Mock
    var mediaClient: MediaClient? = null

    @Mock
    var bookmarkDao: BookmarkPicturesDao? = null

    @InjectMocks
    var bookmarkPicturesController: BookmarkPicturesController? = null

    /**
     * Init mocks
     */
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        val mockMedia = mockMedia
        whenever(bookmarkDao!!.allBookmarks)
            .thenReturn(mockBookmarkList)
        whenever(
            mediaClient!!.getMedia(
                ArgumentMatchers.anyString()
            )
        )
            .thenReturn(Single.just(mockMedia))
    }

    /**
     * Get mock bookmark list
     * @return
     */
    private val mockBookmarkList: List<Bookmark>
        private get() {
            val list = ArrayList<Bookmark>()
            list.add(Bookmark("File:Test1.jpg", "Maskaravivek", Uri.EMPTY))
            list.add(Bookmark("File:Test2.jpg", "Maskaravivek", Uri.EMPTY))
            return list
        }

    /**
     * Test case where all bookmark pictures are fetched and media is found against it
     */
    @Test
    fun loadBookmarkedPictures() {
        val bookmarkedPictures =
            bookmarkPicturesController!!.loadBookmarkedPictures().blockingGet()
        Assert.assertEquals(2, bookmarkedPictures.size.toLong())
    }

    /**
     * Test case where all bookmark pictures are fetched and only one media is found
     */
    @Test
    fun loadBookmarkedPicturesForNullMedia() {
        whenever(mediaClient!!.getMedia("File:Test1.jpg"))
            .thenReturn(Single.error(NullPointerException("Error occurred")))
        whenever(mediaClient!!.getMedia("File:Test2.jpg"))
            .thenReturn(Single.just(mockMedia))
        val bookmarkedPictures =
            bookmarkPicturesController!!.loadBookmarkedPictures().blockingGet()
        Assert.assertEquals(1, bookmarkedPictures.size.toLong())
    }

    private val mockMedia: Media
        private get() = media(filename="File:Test.jpg")

    /**
     * Test case where current bookmarks don't match the bookmarks in DB
     */
    @Test
    fun needRefreshBookmarkedPictures() {
        val needRefreshBookmarkedPictures =
            bookmarkPicturesController!!.needRefreshBookmarkedPictures()
        Assert.assertTrue(needRefreshBookmarkedPictures)
    }

    /**
     * Test case where the DB is up to date with the bookmarks loaded in the list
     */
    @Test
    fun doNotNeedRefreshBookmarkedPictures() {
        val bookmarkedPictures =
            bookmarkPicturesController!!.loadBookmarkedPictures().blockingGet()
        Assert.assertEquals(2, bookmarkedPictures.size.toLong())
        val needRefreshBookmarkedPictures =
            bookmarkPicturesController!!.needRefreshBookmarkedPictures()
        Assert.assertFalse(needRefreshBookmarkedPictures)
    }
}
