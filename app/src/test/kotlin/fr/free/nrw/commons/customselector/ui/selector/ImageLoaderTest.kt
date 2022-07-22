package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.filepicker.PickedFiles
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import io.reactivex.Single
import org.junit.Assert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.HashMap

/**
 * Image Loader Test.
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(PickedFiles::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@ExperimentalCoroutinesApi
class ImageLoaderTest {

    @Mock
    private lateinit var uri:Uri

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var single: Single<Boolean>

    @Mock
    private lateinit var fileProcessor: FileProcessor

    @Mock
    private lateinit var fileUtilsWrapper: FileUtilsWrapper

    @Mock
    private lateinit var uploadedStatusDao: UploadedStatusDao

    @Mock
    private lateinit var notForUploadStatusDao: NotForUploadStatusDao

    @Mock
    private lateinit var holder: ImageAdapter.ImageViewHolder

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var inputStream: FileInputStream

    @Mock
    private lateinit var contentResolver: ContentResolver

    @ExperimentalCoroutinesApi
    private val testDispacher = TestCoroutineDispatcher()

    private lateinit var imageLoader: ImageLoader;
    private var mapImageSHA1: HashMap<Uri, String> = HashMap()
    private var mapHolderImage : HashMap<ImageAdapter.ImageViewHolder, Image> = HashMap()
    private var mapResult: HashMap<String, ImageLoader.Result> = HashMap()
    private var mapModifiedImageSHA1: HashMap<Image, String> = HashMap()
    private lateinit var image: Image;
    private lateinit var uploadedStatus: UploadedStatus;

    /**
     * Setup before test.
     */
    @Before
    @ExperimentalCoroutinesApi
    fun setup() {
        Dispatchers.setMain(testDispacher)
        MockitoAnnotations.initMocks(this)

        imageLoader =
            ImageLoader(mediaClient, fileProcessor, fileUtilsWrapper, uploadedStatusDao,
                notForUploadStatusDao, context)
        uploadedStatus= UploadedStatus(
            "testSha1",
            "testSha1",
            false,
            false,
            Calendar.getInstance().time
        )
        image = Image(1, "test", uri, "test", 0, "test")

        Whitebox.setInternalState(imageLoader, "mapImageSHA1", mapImageSHA1);
        Whitebox.setInternalState(imageLoader, "mapHolderImage", mapHolderImage);
        Whitebox.setInternalState(imageLoader, "mapModifiedImageSHA1", mapModifiedImageSHA1);
        Whitebox.setInternalState(imageLoader, "mapResult", mapResult);
        Whitebox.setInternalState(imageLoader, "context", context)

        whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(fileUtilsWrapper.getSHA1(inputStream)).thenReturn("testSha1")
    }

    /**
     * Reset Dispatchers.
     */
    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
        testDispacher.cleanupTestCoroutines()
    }

    /**
     * Test queryAndSetView with upload Status as null.
     */
    @Test
    fun testQueryAndSetViewUploadedStatusNull() = testDispacher.runBlockingTest {
        whenever(uploadedStatusDao.getUploadedFromImageSHA1(any())).thenReturn(null)
        whenever(notForUploadStatusDao.find(any())).thenReturn(0)
        mapModifiedImageSHA1[image] = "testSha1"
        mapImageSHA1[uri] = "testSha1"

        mapResult["testSha1"] = ImageLoader.Result.TRUE
        imageLoader.queryAndSetView(holder, image, listOf(image), testDispacher, testDispacher)

        mapResult["testSha1"] = ImageLoader.Result.FALSE
        imageLoader.queryAndSetView(holder, image, listOf(image), testDispacher, testDispacher)
    }

    /**
     * Test queryAndSetView with upload Status not null (ie retrieved from table)
     */
    @Test
    fun testQueryAndSetViewUploadedStatusNotNull() = testDispacher.runBlockingTest {
        whenever(uploadedStatusDao.getUploadedFromImageSHA1(any())).thenReturn(uploadedStatus)
        whenever(notForUploadStatusDao.find(any())).thenReturn(0)
        imageLoader.queryAndSetView(holder, image, listOf(image), testDispacher, testDispacher)
    }

    /**
     * Test getSha1
     */
    @Test
    @ExperimentalCoroutinesApi
    fun testGetSha1() = testDispacher.runBlockingTest {

        PowerMockito.mockStatic(PickedFiles::class.java)
        BDDMockito.given(PickedFiles.pickedExistingPicture(context, image.uri))
            .willReturn(UploadableFile(uri, File("ABC")))


        whenever(fileUtilsWrapper.getFileInputStream("ABC")).thenReturn(inputStream)
        whenever(fileUtilsWrapper.getSHA1(inputStream)).thenReturn("testSha1")

        Assert.assertEquals("testSha1", imageLoader.getSHA1(image, testDispacher));
        whenever(PickedFiles.pickedExistingPicture(context, Uri.parse("test"))).thenReturn(
            uploadableFile
        )

        mapModifiedImageSHA1[image] = "testSha2"
        Assert.assertEquals("testSha2", imageLoader.getSHA1(image, testDispacher));
    }

    /**
     * Test getResultFromUploadedStatus.
     */
    @Test
    fun testGetResultFromUploadedStatus() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "getResultFromUploadedStatus",
            UploadedStatus::class.java)
        func.isAccessible = true

        // test Result.INVALID
        uploadedStatus.lastUpdated = Date(0);
        Assert.assertEquals(ImageLoader.Result.INVALID,
            imageLoader.getResultFromUploadedStatus(uploadedStatus))

        // test Result.TRUE
        uploadedStatus.imageResult = true;
        Assert.assertEquals(ImageLoader.Result.TRUE,
            imageLoader.getResultFromUploadedStatus(uploadedStatus))
    }

}