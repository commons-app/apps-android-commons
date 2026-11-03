package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowToast
import java.io.File

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
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    @Mock
    private lateinit var locationManager: LocationServiceManager

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
     * Verifies cancellation resets upload state and removes the nearby-place preference.
     */
    @Test
    fun canceledPickResetsUploadStateAndRemovesNearbyPlace() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        picker.isInternalUploadInProgress = true

        picker.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onCanceled(FilePicker.ImageSource.GALLERY, 0)
        }

        assertFalse(picker.isInternalUploadInProgress)
        verify(defaultKvStore).remove(PLACE_OBJECT)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    /**
     * Verifies picker errors show the error toast without starting an activity.
     */
    @Test
    fun pickerErrorShowsToastWithoutStartingActivity() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)

        picker.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagePickerError(
                IllegalStateException("picker failed"),
                FilePicker.ImageSource.GALLERY,
                0,
            )
        }

        assertEquals(
            activity.getString(fr.free.nrw.commons.R.string.error_occurred_in_picking_images),
            ShadowToast.getTextOfLatestToast().toString(),
        )
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    /**
     * Verifies successful camera picks include files, place, and pre-capture location, then reset
     * the camera-upload flag for the next pick.
     */
    @Test
    fun successfulCameraPickStartsUploadWithPlaceAndResetsCameraFlag() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        val location = LatLng(12.5, 23.5, 4f)
        val place = Place().apply { entityID = "Q42" }
        val image = UploadableFile(File("picked.jpg"))
        picker.locationManager = locationManager
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(false)
        `when`(defaultKvStore.getBoolean("inAppCameraLocationPref", false)).thenReturn(true)
        `when`(locationManager.getLastLocation()).thenReturn(location)
        `when`(defaultKvStore.getJson(PLACE_OBJECT, Place::class.java)).thenReturn(place)

        picker.initiateCameraPick(activity, locationPermissionLauncher, resultLauncher)

        picker.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.CAMERA_IMAGE, 0)
        }

        val cameraUploadIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(cameraUploadIntent)
        assertEquals(UploadActivity::class.java.name, cameraUploadIntent.component?.className)
        assertEquals(
            listOf(image.file.path),
            cameraUploadIntent
                .getParcelableArrayListExtra<UploadableFile>(UploadActivity.EXTRA_FILES)!!
                .map { it.file.path },
        )
        assertEquals("Q42", cameraUploadIntent.getParcelableExtra<Place>(PLACE_OBJECT)!!.entityID)
        assertEquals(
            location,
            cameraUploadIntent.getParcelableExtra<LatLng>(UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE),
        )
        assertTrue(cameraUploadIntent.getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, false))

        picker.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.GALLERY, 0)
        }

        assertFalse(
            shadowOf(activity)
                .nextStartedActivity
                .getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, true),
        )
    }

    /**
     * Verifies a non-camera pick starts upload without optional nearby-place or camera data.
     */
    @Test
    fun successfulNonCameraPickOmitsPlaceAndCameraData() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        val image = UploadableFile(File("picked.jpg"))
        `when`(defaultKvStore.getJson(PLACE_OBJECT, Place::class.java)).thenReturn(null)

        picker.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.GALLERY, 0)
        }

        val uploadIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(uploadIntent)
        assertFalse(uploadIntent.hasExtra(PLACE_OBJECT))
        assertFalse(uploadIntent.hasExtra(UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE))
        assertFalse(uploadIntent.getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, true))
    }

    /**
     * Verifies camera launch records the camera intent and picker configuration.
     */
    @Test
    fun cameraPickLaunchesCameraAndUsesExternalStoragePreference() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val picker = ContributionImagePicker(defaultKvStore)
        val launcher = resultLauncher
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(false)
        `when`(defaultKvStore.getBoolean("inAppCameraLocationPref", false)).thenReturn(false)

        picker.initiateCameraPick(activity, locationPermissionLauncher, launcher)

        val cameraIntent = argumentCaptor<Intent>()
        verify(launcher).launch(cameraIntent.capture())
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, cameraIntent.firstValue.action)
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
