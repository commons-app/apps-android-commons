package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.same
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboCursor
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Modifier
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
    private lateinit var context: Context;

    @Mock
    private lateinit var imageLoaderListener: ImageLoaderListener

    @Mock
    private lateinit var coroutineScope: CoroutineScope

    private lateinit var imageCursor: RoboCursor
    private lateinit var coroutineContext: CoroutineContext
    private lateinit var projection: List<String>
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
        projection = listOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        setFinalStatic(
                ImageFileLoader::class.java.getDeclaredField("coroutineContext"),
                coroutineContext)
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
        val func = imageFileLoader.javaClass.getDeclaredMethod(
            "getImages",
            ImageLoaderListener::class.java
        )
        func.isAccessible = true

        val image1 = arrayOf(1, "imageLoaderTestFile", "src/test/resources/imageLoaderTestFile", 1, "downloads")
        val image2 = arrayOf(2, "imageLoaderTestFile", null, 1, "downloads")
        File("src/test/resources/imageLoaderTestFile").createNewFile()

        imageCursor.setColumnNames(projection)
        imageCursor.setResults(arrayOf(image1, image2));

        val contentResolver: ContentResolver = mock {
            on {
                query(
                    same(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull(),
                    anyOrNull()
                )
            } doReturn imageCursor;
        }

        // test null cursor.
        `when`(
            context.contentResolver
        ).thenReturn(mockContentResolver)
        func.invoke(imageFileLoader, imageLoaderListener);

        // test demo cursor.
        `when`(
            context.contentResolver
        ).thenReturn(contentResolver)
        func.invoke(imageFileLoader, imageLoaderListener);
    }
}