package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentResolver
import android.content.Context
import android.os.Looper
import android.provider.MediaStore
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboCursor
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Custom Selector Image File loader test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ImageFileLoaderTest {
    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var imageLoaderListener: ImageLoaderListener

    @Mock
    private lateinit var coroutineScope: CoroutineScope

    private lateinit var imageCursor: RoboCursor
    private lateinit var coroutineContext: CoroutineContext
    private lateinit var projection: Array<String>
    private lateinit var imageFileLoader: ImageFileLoader

    /**
     * Setup before tests.
     */
    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        coroutineContext = Dispatchers.Main
        imageCursor = RoboCursor()
        imageFileLoader = ImageFileLoader(context)
        projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
            )
        setFinalStatic(
            ImageFileLoader::class.java.getDeclaredField("coroutineContext"),
            coroutineContext,
        )
    }

    /**
     * Test loading device images.
     */
    @Test
    fun testLoadDeviceImages() {
        imageFileLoader.loadDeviceImages(imageLoaderListener)
    }

    /**
     * Test get images from the device function.
     */
    @Test
    fun testGetImages() {
        val func =
            imageFileLoader.javaClass.getDeclaredMethod(
                "getImages",
                ImageLoaderListener::class.java,
            )
        func.isAccessible = true

        val image1 = arrayOf(1, "imageLoaderTestFile", "src/test/resources/imageLoaderTestFile", 1, "downloads")
        val image2 = arrayOf(2, "imageLoaderTestFile", null, 1, "downloads")
        File("src/test/resources/imageLoaderTestFile").createNewFile()

        imageCursor.setColumnNames(projection.toList())
        imageCursor.setResults(arrayOf(image1, image2))

        val contentResolver: ContentResolver =
            mock {
                on {
                    query(
                        same(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                    )
                } doReturn imageCursor
            }

        // test null cursor.
        `when`(
            context.contentResolver,
        ).thenReturn(mockContentResolver)
        func.invoke(imageFileLoader, imageLoaderListener)

        // test demo cursor.
        `when`(
            context.contentResolver,
        ).thenReturn(contentResolver)
        func.invoke(imageFileLoader, imageLoaderListener)
    }

    /**
     * verifies that the getSha1 function correctly calculates a hash for a file.
     */
    @Test
    fun testGetSha1() {
        //use reflection to access the private getSha1 method
        val method = imageFileLoader.javaClass.getDeclaredMethod("getSha1", File::class.java)
        method.isAccessible = true

        val testFile = File.createTempFile("test_image", ".jpg")
        testFile.writeText("test data for sha1 calculation")

        val sha1Hash = method.invoke(imageFileLoader, testFile) as String

        //verify hash is not empty and has correct SHA-1 length
        assert(sha1Hash.isNotEmpty())
        assertEquals(40, sha1Hash.length)

        testFile.delete()
    }

    /**
     * verifies that abortLoadImage() cancels the background job.
     */
    @Test
    fun testAbortLoadImage() {
        //startt loading
        imageFileLoader.loadDeviceImages(imageLoaderListener)

        //trigger abort
        imageFileLoader.abortLoadImage()

        //use reflection to check the private loaderJob state
        val field = imageFileLoader.javaClass.getDeclaredField("loaderJob")
        field.isAccessible = true
        val job = field.get(imageFileLoader) as? kotlinx.coroutines.Job

        //veriyf job exists and is cancelled
        assert(job != null)
        assert(job?.isCancelled == true)
    }

    /**
     * updated thee test to verify that fetched images now include SHA-1 data.
     */
    @Test
    @ExperimentalCoroutinesApi
    fun testGetImagesWithSha1() = runBlockingTest {
        val method = imageFileLoader.javaClass.getDeclaredMethod("getImages", ImageLoaderListener::class.java)
        method.isAccessible = true

        val tempFile = File.createTempFile("mock_img", ".jpg")
        val testFilePath = tempFile.absolutePath
        val imageRecord = arrayOf(1L, "mock_img.jpg", testFilePath, 100L, "Camera", 1600000000L)

        imageCursor.setColumnNames(projection.toList())
        imageCursor.setResults(arrayOf(imageRecord))

        `when`(context.contentResolver).thenReturn(mockContentResolver)
        `when`(mockContentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(imageCursor)

        //invoke the loader logic
        method.invoke(imageFileLoader, imageLoaderListener)

        //fix:execute all pending tasks on the Main Looper
        shadowOf(Looper.getMainLooper()).idle()

        //capture the result passed to the listener
        val captor = argumentCaptor<ArrayList<Image>>()
        verify(imageLoaderListener, atLeastOnce()).onImageLoaded(captor.capture())

        val images = captor.firstValue
        assert(images.isNotEmpty())
        //verify that the SHA-1 field in the first image is not empty
        assert(images[0].sha1.isNotEmpty())

        tempFile.delete()
    }
}