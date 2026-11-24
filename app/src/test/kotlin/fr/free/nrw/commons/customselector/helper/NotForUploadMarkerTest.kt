package fr.free.nrw.commons.customselector.helper

import android.content.ContentResolver
import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.upload.FileUtilsWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.InputStream

/**
 * Not For Upload Marker Test.
 */
@ExperimentalCoroutinesApi
class NotForUploadMarkerTest {
    @Mock
    private lateinit var notForUploadStatusDao: NotForUploadStatusDao

    @Mock
    private lateinit var fileUtilsWrapper: FileUtilsWrapper

    @Mock
    private lateinit var contentResolver: ContentResolver

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var notForUploadMarker: NotForUploadMarker
    private lateinit var firstImage: Image
    private lateinit var secondImage: Image

    /**
     * Set up the tests, every image gets its own SHA1.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        notForUploadMarker =
            NotForUploadMarker(
                notForUploadStatusDao,
                fileUtilsWrapper,
                contentResolver,
                testDispatcher,
            )
        firstImage = createImage(1, "firstSha1")
        secondImage = createImage(2, "secondSha1")
    }

    /**
     * Creates an image whose content has the given SHA1.
     */
    private fun createImage(
        id: Long,
        sha1: String,
    ): Image {
        val uri = Mockito.mock(Uri::class.java)
        val inputStream = Mockito.mock(InputStream::class.java)
        whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(fileUtilsWrapper.getSHA1(inputStream)).thenReturn(sha1)
        return Image(id, "image$id", uri, "abc/abc$id", 1, "bucket1")
    }

    /**
     * Test toggle marks the images when none of them is marked.
     */
    @Test
    fun testToggleMarksImagesWhenNoneIsMarked() =
        TestScope(testDispatcher).runTest {
            whenever(notForUploadStatusDao.find(any())).thenReturn(0)

            val marked = notForUploadMarker.toggle(listOf(firstImage, secondImage))

            assertTrue(marked)
            verify(notForUploadStatusDao).insert(NotForUploadStatus("firstSha1"))
            verify(notForUploadStatusDao).insert(NotForUploadStatus("secondSha1"))
            verify(notForUploadStatusDao, never()).deleteNotForUploadWithImageSHA1(any())
        }

    /**
     * Test toggle marks all the images when only some of them are marked.
     */
    @Test
    fun testToggleMarksAllImagesWhenOnlySomeAreMarked() =
        TestScope(testDispatcher).runTest {
            whenever(notForUploadStatusDao.find("firstSha1")).thenReturn(1)
            whenever(notForUploadStatusDao.find("secondSha1")).thenReturn(0)

            val marked = notForUploadMarker.toggle(listOf(firstImage, secondImage))

            assertTrue(marked)
            verify(notForUploadStatusDao).insert(NotForUploadStatus("firstSha1"))
            verify(notForUploadStatusDao).insert(NotForUploadStatus("secondSha1"))
            verify(notForUploadStatusDao, never()).deleteNotForUploadWithImageSHA1(any())
        }

    /**
     * Test toggle unmarks the images when all of them are already marked.
     */
    @Test
    fun testToggleUnmarksImagesWhenAllAreMarked() =
        TestScope(testDispatcher).runTest {
            whenever(notForUploadStatusDao.find(any())).thenReturn(1)

            val marked = notForUploadMarker.toggle(listOf(firstImage, secondImage))

            assertFalse(marked)
            verify(notForUploadStatusDao).deleteNotForUploadWithImageSHA1("firstSha1")
            verify(notForUploadStatusDao).deleteNotForUploadWithImageSHA1("secondSha1")
            verify(notForUploadStatusDao, never()).insert(NotForUploadStatus("firstSha1"))
            verify(notForUploadStatusDao, never()).insert(NotForUploadStatus("secondSha1"))
        }

    /**
     * Test toggle leaves the database untouched when there is no image.
     */
    @Test
    fun testToggleWithoutImages() =
        TestScope(testDispatcher).runTest {
            val marked = notForUploadMarker.toggle(emptyList())

            assertFalse(marked)
            Mockito.verifyNoInteractions(notForUploadStatusDao)
        }
}
