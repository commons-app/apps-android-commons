package fr.free.nrw.commons.contributions

import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
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
 * Tests camera launch configuration and camera-owned upload metadata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ContributionCameraCoordinatorTest {
    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    @Mock
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    @Mock
    private lateinit var locationManager: LocationServiceManager

    private lateinit var cameraCoordinator: ContributionCameraCoordinator

    /**
     * Initializes mocks and the camera coordinator.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cameraCoordinator = ContributionCameraCoordinator(defaultKvStore)
        cameraCoordinator.locationManager = locationManager
    }

    /**
     * Launches camera without external storage and preserves pre-capture location for upload.
     */
    @Test
    fun cameraLaunchRecordsCameraIntentAndUploadMetadata() {
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        val location = LatLng(12.5, 23.5, 4f)
        `when`(defaultKvStore.getBoolean("useExternalStorage", true)).thenReturn(false)
        `when`(defaultKvStore.getBoolean("inAppCameraLocationPref", false)).thenReturn(true)
        `when`(locationManager.getLastLocation()).thenReturn(location)

        cameraCoordinator.initiateCameraPick(activity, locationPermissionLauncher, resultLauncher)

        val cameraIntent = argumentCaptor<Intent>()
        verify(resultLauncher).launch(cameraIntent.capture())
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, cameraIntent.firstValue.action)
        assertFalse(FilePicker.configuration(activity).shouldCopyTakenPhotosToPublicGalleryAppFolder())

        val metadata = cameraCoordinator.takeUploadMetadata()
        assertEquals(location, metadata.locationBeforeImageCapture)
        assertTrue(metadata.isInAppCameraUpload)
        assertFalse(cameraCoordinator.takeUploadMetadata().isInAppCameraUpload)
    }
}
