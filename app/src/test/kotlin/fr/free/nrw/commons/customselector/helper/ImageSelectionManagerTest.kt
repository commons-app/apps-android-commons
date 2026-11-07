package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import fr.free.nrw.commons.customselector.ui.selector.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TreeMap

/**
 * Unit tests for ImageSelectionManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@ExperimentalCoroutinesApi
class ImageSelectionManagerTest {
    @Mock
    private lateinit var imageLoader: ImageLoader

    @Mock
    private lateinit var imageSelectListener: ImageSelectListener

    private lateinit var activity: CustomSelectorActivity
    private lateinit var selectionManager: ImageSelectionManager
    private lateinit var images: ArrayList<Image>
    private lateinit var holder: ImageAdapter.ImageViewHolder
    private var uri: Uri = Mockito.mock(Uri::class.java)
    private lateinit var image: Image
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Set up test dependencies and state.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(testDispatcher)
        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).get()
        selectionManager =
            ImageSelectionManager(
                activity,
                imageSelectListener,
                imageLoader,
                testDispatcher,
                testDispatcher,
            )
        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")
        images = arrayListOf(image)

        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val listItemView: View = inflater.inflate(R.layout.item_custom_selector_image, null, false)
        holder = ImageAdapter.ImageViewHolder(listItemView)
    }

    /**
     * Reset main dispatcher after test.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test single selection property.
     */
    @Test
    fun testSingleSelection() {
        selectionManager.singleSelection = true
        Assertions.assertTrue(selectionManager.singleSelection)
        selectionManager.singleSelection = false
        Assertions.assertFalse(selectionManager.singleSelection)
    }

    /**
     * Test max upload limit property.
     */
    @Test
    fun testMaxUploadLimit() {
        selectionManager.maxUploadLimit = 10
        Assertions.assertEquals(10, selectionManager.maxUploadLimit)
    }

    /**
     * Test setSelectedImages.
     */
    @Test
    fun testSetSelectedImages() {
        selectionManager.setSelectedImages(images)
        Assertions.assertEquals(1, selectionManager.selectedImages.size)
    }

    /**
     * Test clearSelectedImages.
     */
    @Test
    fun testClearSelectedImages() {
        selectionManager.setSelectedImages(arrayListOf(image))
        selectionManager.numberOfSelectedImagesMarkedAsNotForUpload = 2
        selectionManager.clearSelectedImages()
        Assertions.assertEquals(0, selectionManager.selectedImages.size)
        Assertions.assertEquals(0, selectionManager.numberOfSelectedImagesMarkedAsNotForUpload)
    }

    /**
     * Test reset.
     */
    @Test
    fun testReset() {
        selectionManager.setSelectedImages(arrayListOf(image))
        selectionManager.numberOfSelectedImagesMarkedAsNotForUpload = 3
        selectionManager.reset()
        Assertions.assertEquals(0, selectionManager.selectedImages.size)
        Assertions.assertEquals(0, selectionManager.numberOfSelectedImagesMarkedAsNotForUpload)
    }

    /**
     * Test isSelected.
     */
    @Test
    fun testIsSelected() {
        Assertions.assertFalse(selectionManager.isSelected(0, image, true, emptyList()))
        selectionManager.setSelectedImages(arrayListOf(image))
        Assertions.assertTrue(selectionManager.isSelected(0, image, true, emptyList()))
        Assertions.assertTrue(selectionManager.isSelected(0, image, false, listOf(image)))
        Assertions.assertFalse(selectionManager.isSelected(1, image, false, listOf(image)))
    }

    /**
     * Test onThumbnailClicked.
     */
    @Test
    fun testOnThumbnailClicked() {
        val map = TreeMap<Int, Image>()
        map[0] = image
        selectionManager.onThumbnailClicked(0, holder, images, map) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
