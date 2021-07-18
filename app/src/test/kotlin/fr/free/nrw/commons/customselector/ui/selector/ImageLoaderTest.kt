package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
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
import junit.framework.Assert
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
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

/**
 * Image Loader Test.
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(PickedFiles::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
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
    private lateinit var holder: ImageAdapter.ImageViewHolder

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var uploadableFile: UploadableFile

    @Mock
    private lateinit var inputStream: FileInputStream

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var image: Image;

    private lateinit var imageLoader: ImageLoader;
    private var mapImageSHA1: HashMap<Image, String> = HashMap()
    private var mapHolderImage : HashMap<ImageAdapter.ImageViewHolder, Image> = HashMap()
    private var mapResult: HashMap<String, ImageLoader.Result> = HashMap()

    /**
     * Setup before test.
     */
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        imageLoader =
            ImageLoader(mediaClient, fileProcessor, fileUtilsWrapper, uploadedStatusDao, context)

        Whitebox.setInternalState(imageLoader, "mapImageSHA1", mapImageSHA1);
        Whitebox.setInternalState(imageLoader, "mapHolderImage", mapHolderImage);
        Whitebox.setInternalState(imageLoader, "mapResult", mapResult);
        Whitebox.setInternalState(imageLoader, "context", context)
    }

    /**
     * Test queryAndSetView.
     */
    @Test
    fun testQueryAndSetView(){
        // TODO
        imageLoader.queryAndSetView(holder,image)
    }

    /**
     * Test querySha1
     */
    @Test
    fun testQuerySha1() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "querySHA1",
            String::class.java
        )
        func.isAccessible = true

        Mockito.`when`(single.blockingGet()).thenReturn(true)
        Mockito.`when`(mediaClient.checkFileExistsUsingSha("testSha1")).thenReturn(single)
        Mockito.`when`(fileUtilsWrapper.getSHA1(any())).thenReturn("testSha1")

        // test without saving in map.
        func.invoke(imageLoader, "testSha1");

        // test with map save.
        mapResult["testSha1"] = ImageLoader.Result.FALSE
        func.invoke(imageLoader, "testSha1");
    }

    /**
     * Test getSha1
     */
    @Test
    @Throws (Exception::class)
    fun testGetSha1() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "getSHA1",
            Image::class.java
        )
        func.isAccessible = true

        PowerMockito.mockStatic(PickedFiles::class.java);
        BDDMockito.given(PickedFiles.pickedExistingPicture(context, image.uri))
            .willReturn(UploadableFile(uri, File("ABC")));

        whenever(fileUtilsWrapper.getFileInputStream("ABC")).thenReturn(inputStream)
        whenever(fileUtilsWrapper.getSHA1(inputStream)).thenReturn("testSha1")

        Assert.assertEquals("testSha1", func.invoke(imageLoader, image));
        whenever(PickedFiles.pickedExistingPicture(context,Uri.parse("test"))).thenReturn(uploadableFile)

        mapImageSHA1[image] = "testSha2"
        Assert.assertEquals("testSha2", func.invoke(imageLoader, image));
    }

    /**
     * Test insertIntoUploaded Function.
     */
    @Test
    @Throws (Exception::class)
    fun testInsertIntoUploaded() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "insertIntoUploaded",
            String::class.java,
            String::class.java,
            Boolean::class.java,
            Boolean::class.java)
        func.isAccessible = true

        func.invoke(imageLoader, "", "", true, true)
    }

    /**
     * Test getImageSha1.
     */
    @Test
    @Throws (Exception::class)
    fun testGetImageSHA1() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "getImageSHA1",
            Uri::class.java)
        func.isAccessible = true

        whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(fileUtilsWrapper.getSHA1(inputStream)).thenReturn("testSha1")

        Assert.assertEquals("testSha1", func.invoke(imageLoader,uri))
    }

    /**
     * Test getResultFromUploadedStatus.
     */
    @Test
    @Throws (Exception::class)
    fun testGetResultFromUploadedStatus() {
        val func = imageLoader.javaClass.getDeclaredMethod(
            "getResultFromUploadedStatus",
            UploadedStatus::class.java)
        func.isAccessible = true

        // test Result.TRUE
        Assert.assertEquals(ImageLoader.Result.TRUE,
            func.invoke(imageLoader,
                UploadedStatus("", "", true, true)))

        // test Result.FALSE
        Assert.assertEquals(ImageLoader.Result.FALSE,
            func.invoke(imageLoader,
                UploadedStatus("", "", false, false, Calendar.getInstance().time)))

        // test Result.INVALID
        Assert.assertEquals(ImageLoader.Result.INVALID,
            func.invoke(imageLoader, UploadedStatus("", "", false, false, Date(0))))

    }
}