package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Tests the behavior of contribution image picker workflows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionImagePickerTest {
    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    @Mock
    private lateinit var callbacks: FilePicker.Callbacks

    /**
     * Initializes mocks for picker behavior tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    /**
     * Verifies gallery selection honors the picker preference and configures upload selection.
     */
    @Test
    fun galleryPickUsesDocumentPickerPreferenceAndUpdatesConfiguration() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        val intentCaptor = argumentCaptor<Intent>()
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(true)
        `when`(defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true)).thenReturn(true)

        picker.initiateGalleryPick(activity, resultLauncher, true)

        verify(resultLauncher).launch(intentCaptor.capture())
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intentCaptor.firstValue.action)
        assertTrue(
            FilePicker
                .configuration(activity)
                .allowsMultiplePickingInGallery(),
        )
        assertTrue(
            FilePicker
                .configuration(activity)
                .shouldCopyTakenPhotosToPublicGalleryAppFolder(),
        )
    }

    /**
     * Verifies disabling external storage prevents picker configuration from copying to the gallery.
     */
    @Test
    fun galleryPickDoesNotCopyToPublicGalleryWhenExternalStorageIsDisabled() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(false)
        `when`(defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true)).thenReturn(true)

        picker.initiateGalleryPick(activity, resultLauncher, true)

        assertTrue(FilePicker.configuration(activity).allowsMultiplePickingInGallery())
        assertFalse(FilePicker.configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder())
    }

    /**
     * Verifies gallery result cancellation is dispatched to the document handler when preferred.
     */
    @Test
    fun galleryResultUsesDocumentHandlerWhenPreferred() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        `when`(defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true)).thenReturn(true)

        picker.onPictureReturnedFromGallery(
            ActivityResult(Activity.RESULT_CANCELED, null),
            activity,
            callbacks,
        )

        verify(callbacks).onCanceled(FilePicker.ImageSource.DOCUMENTS, 0)
    }

    /**
     * Verifies gallery result cancellation is dispatched to the gallery handler when preferred.
     */
    @Test
    fun galleryResultUsesGalleryHandlerWhenDocumentPickerIsNotPreferred() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        `when`(defaultKvStore.getBoolean("openDocumentPhotoPickerPref", true)).thenReturn(false)

        picker.onPictureReturnedFromGallery(
            ActivityResult(Activity.RESULT_CANCELED, null),
            activity,
            callbacks,
        )

        verify(callbacks).onCanceled(FilePicker.ImageSource.GALLERY, 0)
    }
}
