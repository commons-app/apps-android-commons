package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.model.Image
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

/**
 * Custom Selector View Model test.
 */
class CustomSelectorViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CustomSelectorViewModel

    @Mock
    private lateinit var imageFileLoader: ImageFileLoader

    @Mock
    private lateinit var context: Context

    /**
     * Set up the test.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        viewModel = CustomSelectorViewModel(context, imageFileLoader)
    }

    /**
     * Test onFullScreenDataReceived replaces the selected images with the ones from full screen mode.
     */
    @Test
    fun testOnFullScreenDataReceived() {
        val images =
            arrayListOf(Image(1, "image", Mockito.mock(Uri::class.java), "abc/abc", 1, "bucket1"))
        val intent = Mockito.mock(Intent::class.java)
        whenever(intent.getParcelableArrayListExtra<Image>(CustomSelectorConstants.NEW_SELECTED_IMAGES))
            .thenReturn(images)

        viewModel.onFullScreenDataReceived(ActivityResult(Activity.RESULT_OK, intent))

        assertEquals(images, viewModel.selectedImages.value)
    }

    /**
     * Test onFullScreenDataReceived keeps the selected images when full screen mode is canceled.
     */
    @Test
    fun testOnFullScreenDataReceivedWhenCanceled() {
        viewModel.onFullScreenDataReceived(ActivityResult(Activity.RESULT_CANCELED, null))

        assertNull(viewModel.selectedImages.value)
    }

    /**
     * Test onCleared();
     */
    @Test
    fun testOnCleared() {
        val func = viewModel.javaClass.getDeclaredMethod("onCleared")
        func.isAccessible = true
        func.invoke(viewModel)
    }
}
