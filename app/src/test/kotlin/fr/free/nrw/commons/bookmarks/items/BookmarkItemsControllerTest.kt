package fr.free.nrw.commons.bookmarks.items

import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.ArrayList

class BookmarkItemsControllerTest {
    @Mock
    var bookmarkDao: BookmarkItemsDao? = null
    @InjectMocks
    lateinit var bookmarkItemsController: BookmarkItemsController

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(bookmarkDao!!.allBookmarksItems)
            .thenReturn(mockBookmarkList)
    }

    /**
     * Get mock bookmark list
     * @return list of DepictedItem
     */
    private val mockBookmarkList: List<DepictedItem>
        get() {
            val list = ArrayList<DepictedItem>()
            list.add(
                DepictedItem(
                    "name", "description", "image url", listOf("instance"),
                    listOf(CategoryItem("category name", "category description",
                    "category thumbnail", false)), true, "id")
            )
            return list
        }

    /**
     * Test case where all bookmark items are fetched and media is found against it
     */
    @Test
    fun loadBookmarkedItems() {
        val bookmarkedItems =
            bookmarkItemsController.loadFavoritesItems()
        Assert.assertEquals(1, bookmarkedItems.size.toLong())
    }
}