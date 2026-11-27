package fr.free.nrw.commons.customselector.ui.selector

import android.net.Uri
import android.os.Looper
import androidx.fragment.app.Fragment
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.customselector.helper.FolderUploadStatusChecker
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Checks that the folder list greys out the folders that were finished while their pictures were
 * shown. The pictures are shown on top of the folder list, so it is not resumed when the user
 * comes back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class FolderFragmentHandledFoldersTest {
    private lateinit var activity: CustomSelectorActivity
    private lateinit var folderFragment: FolderFragment
    private lateinit var checker: FolderUploadStatusChecker

    private val folder =
        Folder(1, "Camera", arrayListOf(Image(1, "image", Uri.parse("content://media/1"), "path", 1, "Camera")))

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Before
    fun setUp() {
        OkHttpConnectionFactory.CLIENT = createTestClient()
        activity =
            Robolectric
                .buildActivity(CustomSelectorActivity::class.java)
                .setup()
                .get()
        idle()
        folderFragment =
            activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as FolderFragment

        checker = mock()
        runBlocking { whenever(checker.findHandledFolders(any(), any())).thenReturn(setOf(1L)) }
        folderFragment.folderUploadStatusChecker = checker
        Whitebox.setInternalState(folderFragment, "folders", arrayListOf(folder))
    }

    private fun adapter(): FolderAdapter = Whitebox.getInternalState(folderFragment, "folderAdapter")

    @Test
    fun `folders are checked again when coming back to the folder list`() {
        // Stands in for the pictures of a folder, which are added on top of the folder list
        activity.supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, Fragment())
            .addToBackStack(null)
            .commit()
        idle()
        runBlocking { verify(checker, never()).findHandledFolders(any(), any()) }
        assertFalse(adapter().isFolderHandled(1))

        activity.supportFragmentManager.popBackStack()
        idle()

        runBlocking { verify(checker).findHandledFolders(argThat { any { it.bucketId == 1L } }, any()) }
        assertTrue(adapter().isFolderHandled(1))
    }

    @Test
    fun `folders are not checked once the folder list is gone`() {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, Fragment())
            .addToBackStack(null)
            .commit()
        idle()
        clearInvocations(checker)

        activity.supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, Fragment())
            .addToBackStack(null)
            .commit()
        activity.supportFragmentManager.popBackStack()
        idle()

        runBlocking { verify(checker, never()).findHandledFolders(any(), any()) }
    }
}
