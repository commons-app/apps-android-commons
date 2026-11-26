package fr.free.nrw.commons.customselector.ui.adapter

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.ImageViewMode
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.util.TreeMap

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
    private lateinit var images: ArrayList<Image>
    private lateinit var holder: ImageAdapter.ImageViewHolder
    private lateinit var selectedImageField: Field
    private var uri: Uri = Mockito.mock(Uri::class.java)
    private lateinit var image: Image
    private val testDispatcher = StandardTestDispatcher()

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

        selectedImageField = imageAdapter.selectionManager.javaClass.getDeclaredField("selectedImages")
        selectedImageField.isAccessible = true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test on create view holder.
     */
    @Test
    fun onCreateViewHolder() {
        imageAdapter.createViewHolder(GridLayout(activity), 0)
    }

    /**
     * Test that list mode inflates the row with the extra detail views.
     */
    @Test
    fun `onCreateViewHolder uses list layout in list mode`() {
        images.add(image)
        imageAdapter.selectionManager.setSelectedImages(images)
        imageAdapter.viewMode = ImageViewMode.LIST

        val listHolder = imageAdapter.createViewHolder(GridLayout(activity), 0)

        Assertions.assertNotNull(listHolder.itemView.findViewById<View>(R.id.image_name))
        Assertions.assertNotNull(listHolder.itemView.findViewById<View>(R.id.location_indicator))
        Assertions.assertEquals(1, imageAdapter.selectionManager.selectedImages.size)
    }

    /**
     * Test that metadata and the geolocation indicator are bound to list rows.
     */
    @Test
    fun `list holder binds metadata and geolocation indicator`() {
        val listItemView =
            LayoutInflater
                .from(activity)
                .inflate(R.layout.item_custom_selector_image_list, null, false)
        val listHolder = ImageAdapter.ImageViewHolder(listItemView)

        listHolder.imageName?.text = "photo.jpg"
        listHolder.imageDetails?.text = "4032 × 3024 · 2.1 MB"
        listHolder.locationIndicator?.visibility = View.VISIBLE
        Assertions.assertEquals("photo.jpg", listItemView.findViewById<TextView>(R.id.image_name).text.toString())
        Assertions.assertEquals(
            "4032 × 3024 · 2.1 MB",
            listItemView.findViewById<TextView>(R.id.image_details).text.toString(),
        )
        Assertions.assertEquals(
            View.VISIBLE,
            listItemView.findViewById<View>(R.id.location_indicator).visibility,
        )

        listHolder.locationIndicator?.visibility = View.GONE
        Assertions.assertEquals(
            View.GONE,
            listItemView.findViewById<View>(R.id.location_indicator).visibility,
        )
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
        selectedImageField.set(imageAdapter.selectionManager, images)
        imageAdapter.onBindViewHolder(holder, 0)
    }

    /**
     * Test on bind view holder with an image whose file was deleted from the
     * device: the adapter must shrink its item count accordingly so the
     * RecyclerView never binds a position that no longer exists.
     */
    @Test
    fun `onBindViewHolder removes deleted image from item count`() {
        // The deletion is processed through itemView.post(), which only runs
        // for a view attached to a window, so use a real activity here.
        val createdActivity =
            Robolectric
                .buildActivity(Activity::class.java)
                .setup()
                .get()
        val inflater =
            createdActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView: View = inflater.inflate(R.layout.item_custom_selector_image, null, false)
        createdActivity.setContentView(itemView)
        val attachedHolder = ImageAdapter.ImageViewHolder(itemView)
        val adapter = ImageAdapter(createdActivity, imageSelectListener, imageLoader)

        // The content resolver returns a null type for these URIs, so the
        // adapter treats them as deleted from the device.
        val deletedUri: Uri = Uri.parse("content://media/external/images/media/1")
        val otherUri: Uri = Uri.parse("content://media/external/images/media/2")
        images.add(Image(1, "image", deletedUri, "abc/abc", 1, "bucket1"))
        images.add(Image(2, "image2", otherUri, "abc/def", 1, "bucket1"))
        adapter.init(images, images, TreeMap())

        adapter.onBindViewHolder(attachedHolder, 0)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        Assertions.assertEquals(1, adapter.itemCount)

        // Binding the former last position must not throw
        // IndexOutOfBoundsException.
        adapter.onBindViewHolder(attachedHolder, 1)
    }

    /**
     * Test processThumbnailForActionedImage
     */
    @Test
    fun processThumbnailForActionedImage() =
        runBlocking {
            Whitebox.setInternalState(imageAdapter, "allImages", listOf(image))
            whenever(
                imageLoader.nextActionableImage(
                    listOf(image),
                    Dispatchers.IO,
                    Dispatchers.Default,
                    0,
                    emptyList(),
                ),
            ).thenReturn(0)
            imageAdapter.processThumbnailForActionedImage(holder, 0, emptyList())
        }

    /**
     * Test processThumbnailForActionedImage
     */
    @Test
    fun `processThumbnailForActionedImage when reached end of the folder`() =
        runBlocking {
            whenever(
                imageLoader.nextActionableImage(
                    ArrayList(),
                    Dispatchers.IO,
                    Dispatchers.Default,
                    0,
                    emptyList(),
                ),
            ).thenReturn(-1)
            imageAdapter.processThumbnailForActionedImage(holder, 0, emptyList())
        }

    /**
     * Test init.
     */
    @Test
    fun init() {
        imageAdapter.init(images, images, TreeMap())
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
        imageAdapter.selectionManager.setSelectedImages(images)
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
     * Test imageIds
     */
    @Test
    fun imageIds() {
        imageAdapter.init(listOf(image), listOf(image), TreeMap())
        Assertions.assertEquals(1, imageAdapter.imageIds[0])
    }
}
