package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Folder Browser Test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FolderBrowserTest {
    @Mock
    private lateinit var viewModel: CustomSelectorViewModel

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var folderDeletionLauncher: ActivityResultLauncher<IntentSenderRequest>

    private lateinit var activity: CustomSelectorActivity
    private lateinit var folderBrowser: FolderBrowser
    private var uploadLimitErrorClicks = 0

    /**
     * Set up the tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()

        activity =
            Robolectric
                .buildActivity(CustomSelectorActivity::class.java)
                .get()
        val onCreate = activity.javaClass.getDeclaredMethod("onCreate", Bundle::class.java)
        onCreate.isAccessible = true
        onCreate.invoke(activity, null)

        folderBrowser =
            FolderBrowser(
                activity,
                viewModel,
                defaultKvStore,
                folderDeletionLauncher,
            ) { uploadLimitErrorClicks++ }
    }

    /**
     * Returns the text of the toolbar title.
     */
    private fun titleText(): String = activity.findViewById<TextView>(R.id.title).text.toString()

    /**
     * Returns the overflow menu button of the toolbar.
     */
    private fun overflowMenu(): ImageButton = activity.findViewById(R.id.menu_overflow)

    /**
     * Test changeTitle changes the title of the toolbar.
     */
    @Test
    fun testChangeTitle() {
        folderBrowser.changeTitle("folder", 0)

        assertEquals("folder", titleText())
    }

    /**
     * Test changeTitle appends the number of selected images to the title.
     */
    @Test
    fun testChangeTitleWithSelectedImages() {
        folderBrowser.changeTitle("folder", 2)

        val appendix =
            activity.resources.getQuantityString(
                R.plurals.custom_picker_images_selected_title_appendix,
                2,
                2,
            )
        assertEquals("folder ($appendix)", titleText())
    }

    /**
     * Test changeTitle ignores an empty title.
     */
    @Test
    fun testChangeTitleIgnoresEmptyTitle() {
        folderBrowser.changeTitle("folder", 0)

        folderBrowser.changeTitle("", 3)

        assertEquals("folder", titleText())
    }

    /**
     * Test openFolder keeps track of the opened folder and shows the overflow menu.
     */
    @Test
    fun testOpenFolder() {
        whenever(defaultKvStore.getBoolean("displayDeletionButton")).thenReturn(true)

        folderBrowser.openFolder(1, "folder", 0)

        assertTrue(folderBrowser.isImageFragmentOpen)
        assertEquals(1L, folderBrowser.bucketId)
        assertEquals("folder", folderBrowser.bucketName)
        assertEquals("folder", titleText())
        assertEquals(View.VISIBLE, overflowMenu().visibility)
    }

    /**
     * Test openFolder keeps the overflow menu hidden when the folder deletion button is disabled.
     */
    @Test
    fun testOpenFolderWithoutDeletionButton() {
        folderBrowser.openFolder(1, "folder", 0)

        assertEquals(View.GONE, overflowMenu().visibility)
    }

    /**
     * Test onNavigatedBack hides the overflow menu again.
     */
    @Test
    fun testOnNavigatedBackHidesOverflowMenu() {
        whenever(defaultKvStore.getBoolean("displayDeletionButton")).thenReturn(true)
        folderBrowser.openFolder(1, "folder", 0)

        folderBrowser.onNavigatedBack()

        assertEquals(View.INVISIBLE, overflowMenu().visibility)
    }

    /**
     * Test the upload limit error button of the toolbar notifies the listener.
     */
    @Test
    fun testUploadLimitErrorClick() {
        folderBrowser.setUpViews()

        activity.findViewById<ImageButton>(R.id.image_limit_error).performClick()

        assertEquals(1, uploadLimitErrorClicks)
    }

    /**
     * Test onDeleteFolderResultReceived does nothing when the deletion is not confirmed.
     */
    @Test
    fun testOnDeleteFolderResultReceivedWhenNotConfirmed() {
        folderBrowser.openFolder(1, "folder", 0)

        folderBrowser.onDeleteFolderResultReceived(ActivityResult(Activity.RESULT_CANCELED, null))

        assertTrue(folderBrowser.isImageFragmentOpen)
        Mockito.verifyNoInteractions(viewModel)
    }

    /**
     * Test no folder is opened by default.
     */
    @Test
    fun testNoFolderOpenedByDefault() {
        assertFalse(folderBrowser.isImageFragmentOpen)
        assertEquals(0L, folderBrowser.bucketId)
    }
}
