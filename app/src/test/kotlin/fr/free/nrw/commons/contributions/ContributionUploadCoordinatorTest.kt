package fr.free.nrw.commons.contributions

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
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
 * Tests picker result handling and the upload activity handoff.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionUploadCoordinatorTest {
    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    @Mock
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    @Mock
    private lateinit var locationManager: LocationServiceManager

    private lateinit var cameraCoordinator: ContributionCameraCoordinator
    private lateinit var uploadCoordinator: ContributionUploadCoordinator
    private lateinit var activity: ProfileActivity

    /**
     * Initializes mocks and the coordinators used by upload flow tests.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cameraCoordinator = ContributionCameraCoordinator(defaultKvStore)
        cameraCoordinator.locationManager = locationManager
        uploadCoordinator = ContributionUploadCoordinator(defaultKvStore, cameraCoordinator)
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
    }

    /**
     * Resets upload state and removes the nearby place when a picker result is canceled.
     */
    @Test
    fun canceledPickResetsUploadStateAndRemovesNearbyPlace() {
        uploadCoordinator.isInternalUploadInProgress = true

        uploadCoordinator.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onCanceled(FilePicker.ImageSource.GALLERY, 0)
        }

        assertFalse(uploadCoordinator.isInternalUploadInProgress)
        verify(defaultKvStore).remove(PLACE_OBJECT)
        assertNull(shadowOf(activity).nextStartedActivity)
    }

    /**
     * Shows the picker error toast and does not start the upload activity.
     */
    @Test
    fun pickerErrorShowsToastWithoutStartingActivity() {
        uploadCoordinator.handleActivityResultWithCallback(activity) { resultCallbacks ->
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
     * Includes camera metadata in the upload intent and resets it for the next pick.
     */
    @Test
    fun successfulCameraPickStartsUploadWithPlaceAndLocation() {
        val location = LatLng(12.5, 23.5, 4f)
        val place = Place().apply { entityID = "Q42" }
        val image = UploadableFile(File("picked.jpg"))
        cameraCoordinator.locationManager = locationManager
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(false)
        `when`(defaultKvStore.getBoolean("inAppCameraLocationPref", false)).thenReturn(true)
        `when`(locationManager.getLastLocation()).thenReturn(location)
        `when`(defaultKvStore.getJson(PLACE_OBJECT, Place::class.java)).thenReturn(place)

        cameraCoordinator.initiateCameraPick(activity, locationPermissionLauncher, resultLauncher)
        uploadCoordinator.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.CAMERA_IMAGE, 0)
        }

        val uploadIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(uploadIntent)
        assertEquals(UploadActivity::class.java.name, uploadIntent.component?.className)
        assertEquals(
            listOf(image.file.path),
            uploadIntent
                .getParcelableArrayListExtra<UploadableFile>(UploadActivity.EXTRA_FILES)!!
                .map { it.file.path },
        )
        assertEquals("Q42", uploadIntent.getParcelableExtra<Place>(PLACE_OBJECT)!!.entityID)
        assertEquals(
            location,
            uploadIntent.getParcelableExtra<LatLng>(UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE),
        )
        assertTrue(uploadIntent.getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, false))

        uploadCoordinator.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.GALLERY, 0)
        }

        assertFalse(
            shadowOf(activity)
                .nextStartedActivity
                .getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, true),
        )
    }

    /**
     * Omits optional place and camera data for a non-camera upload with no stored place.
     */
    @Test
    fun successfulNonCameraPickOmitsPlaceAndCameraData() {
        val image = UploadableFile(File("picked.jpg"))
        `when`(defaultKvStore.getJson(PLACE_OBJECT, Place::class.java)).thenReturn(null)

        uploadCoordinator.handleActivityResultWithCallback(activity) { resultCallbacks ->
            resultCallbacks.onImagesPicked(listOf(image), FilePicker.ImageSource.GALLERY, 0)
        }

        val uploadIntent = shadowOf(activity).nextStartedActivity
        assertNotNull(uploadIntent)
        assertFalse(uploadIntent.hasExtra(PLACE_OBJECT))
        assertFalse(uploadIntent.hasExtra(UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE))
        assertFalse(uploadIntent.getBooleanExtra(UploadActivity.IN_APP_CAMERA_UPLOAD, true))
    }
}
