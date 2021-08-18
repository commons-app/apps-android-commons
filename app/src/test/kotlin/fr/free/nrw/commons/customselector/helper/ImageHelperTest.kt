package fr.free.nrw.commons.customselector.helper

import android.net.Uri
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Custom Selector Image Helper Test
 */
internal class ImageHelperTest {

    var uri: Uri = mock(Uri::class.java)
    private val folderImage1 = Image(1, "image1", uri, "abc/abc", 1, "bucket1")
    private val folderImage2 = Image(2, "image1", uri, "xyz/xyz", 2, "bucket2")
    private val mockImageList = ArrayList<Image>(listOf(folderImage1, folderImage2))
    private val folderImageList1 = ArrayList<Image>(listOf(folderImage1))
    private val folderImageList2 = ArrayList<Image>(listOf(folderImage2))

    /**
     * Test folder list from images.
     */
    @Test
    fun folderListFromImages() {
        val folderList = ArrayList<Folder>(listOf(Folder(1, "bucket1", folderImageList1), Folder(2, "bucket2", folderImageList2)))
        assertEquals(folderList, ImageHelper.folderListFromImages(mockImageList))
    }

    /**
     * Test filter images.
     */
    @Test
    fun filterImages() {
        assertEquals(folderImageList1, ImageHelper.filterImages(mockImageList, 1))
    }

    /**
     * Test get index from image list.
     */
    @Test
    fun getIndex() {
        assertEquals(1,ImageHelper.getIndex(mockImageList, folderImage2))
    }

    /**
     * Test get index list.
     */
    @Test
    fun getIndexList() {
        assertEquals(ArrayList<Int>(listOf(0)), ImageHelper.getIndexList(mockImageList, folderImageList2))
    }
}