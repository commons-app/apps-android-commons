package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

/**
 * Custom Selector image adapter test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ImageAdapterTest {
    @Mock
    private lateinit var image: Image
    @Mock
    private lateinit var imageLoader: ImageLoader
    @Mock
    private lateinit var imageSelectListener: ImageSelectListener

    private lateinit var activity: CustomSelectorActivity
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var images : ArrayList<Image>
    private lateinit var holder: ImageAdapter.ImageViewHolder
    private lateinit var selectedImageField: Field

    /**
     * Set up variables.
     */
    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).get()
        imageAdapter = ImageAdapter(activity, imageSelectListener, imageLoader)
        images = ArrayList()

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val listItemView: View = inflater.inflate(R.layout.item_custom_selector_image, null, false)
        holder = ImageAdapter.ImageViewHolder(listItemView)

        selectedImageField = imageAdapter.javaClass.getDeclaredField("selectedImages")
        selectedImageField.isAccessible = true
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
        // Parameters.
        images.add(image)
        imageAdapter.init(images)

        // Test conditions.
        imageAdapter.onBindViewHolder(holder, 0)
        selectedImageField.set(imageAdapter, images)
        imageAdapter.onBindViewHolder(holder, 0)
    }

    /**
     * Test init.
     */
    @Test
    fun init() {
        imageAdapter.init(images)
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
        imageAdapter.init(images)

        // Test conditions
        holder.itemUploaded()
        func.invoke(imageAdapter, holder, 0)
        holder.itemNotUploaded()
        func.invoke(imageAdapter, holder, 0)
        selectedImageField.set(imageAdapter, images)
        func.invoke(imageAdapter, holder, 1)
    }

    /**
     * Test get item count.
     */
    @Test
    fun getItemCount() {
        Assertions.assertEquals(0, imageAdapter.itemCount)
    }
}