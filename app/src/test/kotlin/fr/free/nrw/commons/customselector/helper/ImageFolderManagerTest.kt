package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.customselector.ui.selector.ImageFragment
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for ImageFolderManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ImageFolderManagerTest {
    private lateinit var folderManager: ImageFolderManager
    private lateinit var context: Context
    private lateinit var image1: Image
    private lateinit var image2: Image

    @Mock
    private lateinit var contributionDao: ContributionDao

    @Mock
    private lateinit var imageAdapter: ImageAdapter

    @Mock
    private lateinit var selectionManager: ImageSelectionManager

    @Mock
    private lateinit var contribution: Contribution

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        val mockUri = Mockito.mock(Uri::class.java)
        image1 = Image(1L, "img1.jpg", mockUri, "path/1", 100L)
        image2 = Image(2L, "img2.jpg", mockUri, "path/2", 200L)
        whenever(imageAdapter.selectionManager).thenReturn(selectionManager)
        folderManager = ImageFolderManager(contributionDao)
    }

    @Test
    fun testGetUploadingContributionsWhenSuccessful() {
        whenever(contributionDao.getContribution(any<List<Int>>()))
            .thenReturn(Single.just(listOf(contribution)))

        val result = folderManager.getUploadingContributions()

        assertEquals(1, result.size)
        assertEquals(contribution, result[0])
    }

    @Test
    fun testGetUploadingContributionsWhenNullReturnsEmpty() {
        whenever(contributionDao.getContribution(any<List<Int>>())).thenReturn(null)

        val result = folderManager.getUploadingContributions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun testOnSwitchChangedTrue() {
        val prefs: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, Context.MODE_PRIVATE)
        whenever(contributionDao.getContribution(any<List<Int>>()))
            .thenReturn(Single.just(emptyList()))

        val allImages = arrayListOf(image1, image2)
        folderManager.onSwitchChanged(true, allImages, imageAdapter, context)

        assertTrue(ImageFragment.showAlreadyActionedImages)
        assertTrue(prefs.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, false))
        verify(imageAdapter).notifyDataSetChanged()
    }

    @Test
    fun testOnSwitchChangedFalse() {
        val prefs: SharedPreferences =
            context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, Context.MODE_PRIVATE)
        whenever(contributionDao.getContribution(any<List<Int>>()))
            .thenReturn(Single.just(emptyList()))

        val allImages = arrayListOf(image1, image2)
        folderManager.onSwitchChanged(false, allImages, imageAdapter, context)

        assertEquals(false, ImageFragment.showAlreadyActionedImages)
        assertEquals(false, prefs.getBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true))
        verify(imageAdapter).notifyDataSetChanged()
    }

    @Test
    fun testFilterImages() {
        val images = arrayListOf(image1, image2)
        val filtered = folderManager.filterImages(images, 100L)

        assertEquals(1, filtered.size)
        assertEquals(image1, filtered[0])
    }

    @Test
    fun testProcessResultImages() {
        whenever(contributionDao.getContribution(any<List<Int>>()))
            .thenReturn(Single.just(emptyList()))

        val images = arrayListOf(image1)
        val selectedImages = arrayListOf(image1)
        val result = folderManager.processResultImages(images, 100L, imageAdapter, selectedImages)

        assertEquals(1, result.size)
        verify(selectionManager).setSelectedImages(selectedImages)
        verify(imageAdapter).notifyDataSetChanged()
    }

    @Test
    fun testSaveLastVisibleItemIdValidPosition() {
        val prefs: SharedPreferences =
            context.getSharedPreferences("CustomSelector", Context.MODE_PRIVATE)
        whenever(imageAdapter.getImageIdAt(2)).thenReturn(12345L)

        val filteredImages = listOf(image1, image2)
        folderManager.saveLastVisibleItemId(2, filteredImages, imageAdapter, context)

        assertEquals(12345L, prefs.getLong("ItemId", -1L))
    }

    @Test
    fun testSaveLastVisibleItemIdInvalidPosition() {
        val prefs: SharedPreferences =
            context.getSharedPreferences("CustomSelector", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val filteredImages = listOf(image1)
        folderManager.saveLastVisibleItemId(-1, filteredImages, imageAdapter, context)

        assertEquals(-1L, prefs.getLong("ItemId", -1L))
    }

    @Test
    fun testCleanupWithAdapter() {
        folderManager.cleanup(imageAdapter)
        verify(imageAdapter).cleanUp()
    }

    @Test
    fun testCleanupWithNullAdapter() {
        folderManager.cleanup(null)
        // Should complete without exception
    }
}
