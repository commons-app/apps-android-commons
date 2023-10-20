package fr.free.nrw.commons.customselector.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList

/**
 * Custom Selector image adapter test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@ExperimentalCoroutinesApi
class ImageAdapterTest {
    @Mock
    private lateinit var imageLoader: ImageLoader
    @Mock
    private lateinit var imageSelectListener: ImageSelectListener
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var mockContentResolver: ContentResolver
    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var activity: CustomSelectorActivity
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var images : ArrayList<Image>
    private lateinit var holder: ImageAdapter.ImageViewHolder
    private lateinit var selectedImageField: Field
    private var uri: Uri = Mockito.mock(Uri::class.java)
    private lateinit var image: Image
    private val testDispatcher = TestCoroutineDispatcher()


    /**
     * Set up variables.
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(testDispatcher)
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).get()
        imageAdapter = ImageAdapter(activity, imageSelectListener, imageLoader)
        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")
        images = ArrayList()

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val listItemView: View = inflater.inflate(R.layout.item_custom_selector_image, null, false)
        holder = ImageAdapter.ImageViewHolder(listItemView)

        selectedImageField = imageAdapter.javaClass.getDeclaredField("selectedImages")
        selectedImageField.isAccessible = true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    /**
     * Test on create view holder.
     */
    @Test
    fun onCreateViewHolder() {
        imageAdapter.createViewHolder(GridLayout(activity), 0)
    }

    /**
     * Test on bind view holder.
     */
    @Test
    fun onBindViewHolder() {

        whenever(context.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContentResolver.getType(uri)).thenReturn("jpg")
        // Parameters.
        images.add(image)
        imageAdapter.init(images, images, TreeMap())

        whenever(context.getSharedPreferences("custom_selector", 0))
            .thenReturn(sharedPreferences)
        // Test conditions.
        imageAdapter.onBindViewHolder(holder, 0)
        selectedImageField.set(imageAdapter, images)
        imageAdapter.onBindViewHolder(holder, 0)
    }

    /**
     * Test processThumbnailForActionedImage
     */
    @Test
    fun processThumbnailForActionedImage() = runBlocking {
        Whitebox.setInternalState(imageAdapter, "allImages", listOf(image))
        whenever(imageLoader.nextActionableImage(listOf(image), Dispatchers.IO, Dispatchers.Default,
        0)).thenReturn(0)
        imageAdapter.processThumbnailForActionedImage(holder, 0)
    }

    /**
     * Test processThumbnailForActionedImage
     */
    @Test
    fun `processThumbnailForActionedImage when reached end of the folder`() = runBlocking {
        whenever(imageLoader.nextActionableImage(ArrayList(), Dispatchers.IO, Dispatchers.Default,
            0)).thenReturn(-1)
        imageAdapter.processThumbnailForActionedImage(holder, 0)
    }

    /**
     * Test init.
     */
    @Test
    fun init() {
        imageAdapter.init(images, images, TreeMap())
    }

    /**
     * Test private function select or remove image.
     */
    @Test
    fun selectOrRemoveImage() {
        // Access function
        val func = imageAdapter.javaClass.getDeclaredMethod("selectOrRemoveImage", ImageAdapter.ImageViewHolder::class.java, Int::class.java)
        func.isAccessible = true

        // Parameters
        images.addAll(listOf(image, image))
        imageAdapter.init(images, images, TreeMap())

        // Test conditions
        holder.itemUploaded()
        func.invoke(imageAdapter, holder, 0)
        holder.itemNotUploaded()
        holder.itemNotForUpload()
        func.invoke(imageAdapter, holder, 0)
        holder.itemNotForUpload()
        func.invoke(imageAdapter, holder, 0)
        selectedImageField.set(imageAdapter, images)
        func.invoke(imageAdapter, holder, 1)
    }

    /**
     * Test private function onThumbnailClicked.
     */
    @Test
    fun onThumbnailClicked() {
        images.add(image)
        Whitebox.setInternalState(imageAdapter, "images", images)
        // Access function
        val func = imageAdapter.javaClass.getDeclaredMethod(
            "onThumbnailClicked",
            Int::class.java,
            ImageAdapter.ImageViewHolder::class.java
        )
        func.isAccessible = true
        func.invoke(imageAdapter, 0, holder)
    }

    /**
     * Test get item count.
     */
    @Test
    fun getItemCount() {
        Assertions.assertEquals(0, imageAdapter.itemCount)
    }

    /**
     * Test setSelectedImages.
     */
    @Test
    fun setSelectedImages() {
        images.add(image)
        imageAdapter.setSelectedImages(images)
    }

    /**
     * Test refresh.
     */
    @Test
    fun refresh() {
        imageAdapter.refresh(listOf(image), listOf(image))
    }

    /**
     * Test getSectionName.
     */
    @Test
    fun getSectionName() {
        images.add(image)
        Whitebox.setInternalState(imageAdapter, "images", images)
        Assertions.assertEquals("", imageAdapter.getSectionName(0))
    }

    /**
     * Test cleanUp.
     */
    @Test
    fun cleanUp() {
        imageAdapter.cleanUp()
    }

    /**
     * Test getImageId
     */
    @Test
    fun getImageIdAt() {
        imageAdapter.init(listOf(image), listOf(image), TreeMap())
        Assertions.assertEquals(1, imageAdapter.getImageIdAt(0))
    }
}