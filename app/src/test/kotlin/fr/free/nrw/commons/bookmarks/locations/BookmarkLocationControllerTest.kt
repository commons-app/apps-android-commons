package fr.free.nrw.commons.bookmarks.locations

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.nearby.Place
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.ArrayList

class BookmarkLocationControllerTest {
    @Mock
    var bookmarkDao: BookmarkLocationsDao? = null
    @InjectMocks
    lateinit var bookmarkLocationsController: BookmarkLocationsController

     @Before
     fun setup() {
         MockitoAnnotations.initMocks(this)
         whenever(bookmarkDao!!.allBookmarksLocations)
             .thenReturn(mockBookmarkList)
     }

    /**
     * Get mock bookmark list
     * @return
     */
    private val mockBookmarkList: List<Place>
        private get() {
            val list = ArrayList<Place>()
            list.add(
                Place(
                    "en", "a place", null, "a description", null, "a cat", null, null, true, "entityID")
            )
            list.add(
                Place(
                    "en",
                    "another place",
                    null,
                    "another description",
                    null,
                    "another cat",
                    null,
                    null,
                    true,
                    "entityID"
                )
            )
            return list
        }

    /**
     * Test case where all bookmark locations are fetched and media is found against it
     */
    @Test
    fun loadBookmarkedLocations() {
        val bookmarkedLocations =
            bookmarkLocationsController!!.loadFavoritesLocations()
        Assert.assertEquals(2, bookmarkedLocations.size.toLong())
    }
}