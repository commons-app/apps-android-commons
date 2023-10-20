package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.github.chrisbanes.photoview.PhotoView
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.nhaarman.mockitokotlin2.mock
import fr.free.nrw.commons.LocationPicker.LocationPicker
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowIntent
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class UploadMediaDetailFragmentUnitTest {

    private lateinit var fragment: UploadMediaDetailFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var view: View
    private lateinit var runnable: Runnable

    private lateinit var tvTitle: TextView
    private lateinit var tooltip: ImageView
    private lateinit var rvDescriptions: RecyclerView
    private lateinit var btnPrevious: AppCompatButton
    private lateinit var btnNext: AppCompatButton
    private lateinit var btnCopyToSubsequentMedia: AppCompatButton
    private lateinit var photoViewBackgroundImage: PhotoView
    private lateinit var ibMap: AppCompatImageButton
    private lateinit var llContainerMediaDetail: LinearLayout
    private lateinit var ibExpandCollapse: AppCompatImageButton

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var callback: UploadMediaDetailFragment.UploadMediaDetailFragmentCallback

    @Mock
    private lateinit var presenter: UploadMediaDetailsContract.UserActionListener

    @Mock
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter

    @Mock
    private lateinit var uploadItem: UploadItem

    @Mock
    private lateinit var mediaUri: Uri

    @Mock
    private lateinit var place: Place

    @Mock
    private var location: fr.free.nrw.commons.location.LatLng? = null

    @Mock
    private lateinit var defaultKvStore: JsonKvStore

    @Mock
    private lateinit var imageCoordinates: ImageCoordinates

    private lateinit var activity: UploadActivity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(UploadActivity::class.java).create().get()
        layoutInflater = LayoutInflater.from(activity)

        view = LayoutInflater.from(activity)
            .inflate(R.layout.fragment_upload_media_detail_fragment, null) as View

        fragment = UploadMediaDetailFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        tvTitle = view.findViewById(R.id.tv_title)
        tooltip = view.findViewById(R.id.tooltip)
        rvDescriptions = view.findViewById(R.id.rv_descriptions)
        btnPrevious = view.findViewById(R.id.btn_previous)
        btnNext = view.findViewById(R.id.btn_next)
        btnCopyToSubsequentMedia = view.findViewById(R.id.btn_copy_subsequent_media)
        photoViewBackgroundImage = view.findViewById(R.id.backgroundImage)
        ibMap = view.findViewById(R.id.ib_map)
        llContainerMediaDetail = view.findViewById(R.id.ll_container_media_detail)
        ibExpandCollapse = view.findViewById(R.id.ib_expand_collapse)

        Whitebox.setInternalState(fragment, "tvTitle", tvTitle)
        Whitebox.setInternalState(fragment, "tooltip", tooltip)
        Whitebox.setInternalState(fragment, "callback", callback)
        Whitebox.setInternalState(fragment, "rvDescriptions", rvDescriptions)
        Whitebox.setInternalState(fragment, "btnPrevious", btnPrevious)
        Whitebox.setInternalState(fragment, "btnNext", btnNext)
        Whitebox.setInternalState(fragment, "btnCopyToSubsequentMedia", btnCopyToSubsequentMedia)
        Whitebox.setInternalState(fragment, "photoViewBackgroundImage", photoViewBackgroundImage)
        Whitebox.setInternalState(fragment, "uploadMediaDetailAdapter", uploadMediaDetailAdapter)
        Whitebox.setInternalState(fragment, "ibMap", ibMap)
        Whitebox.setInternalState(fragment, "llContainerMediaDetail", llContainerMediaDetail)
        Whitebox.setInternalState(fragment, "ibExpandCollapse", ibExpandCollapse)
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCallback() {
        fragment.setCallback(null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreate() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onCreate(Bundle())
    }

    @Test
    @Throws(Exception::class)
    fun testSetImageTobeUploaded() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.setImageTobeUploaded(null, null, location)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        fragment.onCreateView(layoutInflater, null, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testOnViewCreated() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        fragment.onViewCreated(view, savedInstanceState)
    }

    @Test
    @Throws(Exception::class)
    fun testInit() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        val method: Method = UploadMediaDetailFragment::class.java.getDeclaredMethod(
            "init"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseGetIndexInViewFlipperNonZero() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        `when`(callback.getIndexInViewFlipper(fragment)).thenReturn(1)
        `when`(callback.totalNumberOfSteps).thenReturn(5)
        val method: Method = UploadMediaDetailFragment::class.java.getDeclaredMethod(
            "init"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowInfoAlert() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = UploadMediaDetailFragment::class.java.getDeclaredMethod(
            "showInfoAlert", Int::class.java, Int::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, R.string.media_detail_step_title, R.string.media_details_tooltip)
    }

    @Test
    @Throws(Exception::class)
    fun testOnNextButtonClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        fragment.onNextButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnPreviousButtonClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        fragment.onPreviousButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnButtonAddDescriptionClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onButtonAddDescriptionClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testShowSimilarImageFragment() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val similar: ImageCoordinates = mock()
        fragment.showSimilarImageFragment("original", "possible", similar)
    }

    @Test
    @Throws(Exception::class)
    fun testOnImageProcessed() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(uploadItem.mediaUri).thenReturn(mediaUri)
        fragment.onImageProcessed(uploadItem, place)
    }

    @Test
    @Throws(Exception::class)
    fun testOnNearbyPlaceFound() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onNearbyPlaceFound(uploadItem, place)
    }

    @Test
    @Throws(Exception::class)
    fun testShowProgress() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showProgress(true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnImageValidationSuccess() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onImageValidationSuccess()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBecameVisible() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        Whitebox.setInternalState(fragment, "showNearbyFound", true)
        Whitebox.setInternalState(fragment, "nearbyPlace", place)
        Whitebox.setInternalState(fragment, "uploadItem", uploadItem)
        val method: Method = UploadMediaDetailFragment::class.java.getDeclaredMethod(
            "onBecameVisible"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageCaseOne() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showMessage(R.string.add_caption_toast, R.color.color_error)
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageCaseTwo() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showMessage("", R.color.color_error)
    }

    @Test
    @Throws(Exception::class)
    fun testShowDuplicatePicturePopupCaseDefault() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showDuplicatePicturePopup(uploadItem)
    }

    @Test
    @Throws(Exception::class)
    fun testShowDuplicatePicturePopupCaseFalse() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "defaultKvStore", defaultKvStore)
        Whitebox.setInternalState(fragment, "presenter", presenter)
        `when`(defaultKvStore.getBoolean("showDuplicatePicturePopup", true)).thenReturn(false)
        fragment.showDuplicatePicturePopup(uploadItem)
    }

    @Test
    @Throws(Exception::class)
    fun testShowBadImagePopup() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showBadImagePopup(8, uploadItem)
    }

    @Test
    @Throws(Exception::class)
    fun testShowConnectionErrorPopup() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.showConnectionErrorPopup()
    }

    @Test
    @Throws(Exception::class)
    fun testShowExternalMap() {
        shadowOf(Looper.getMainLooper()).idle()
        `when`(uploadItem.gpsCoords).thenReturn(imageCoordinates)
        `when`(imageCoordinates.decLatitude).thenReturn(0.0)
        `when`(imageCoordinates.decLongitude).thenReturn(0.0)
        `when`(imageCoordinates.zoomLevel).thenReturn(16.0)
        fragment.showExternalMap(uploadItem)
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResultOnMapIconClicked() {
        shadowOf(Looper.getMainLooper()).idle()
        Mockito.mock(LocationPicker::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val cameraPosition = Mockito.mock(CameraPosition::class.java)
        val latLng = Mockito.mock(LatLng::class.java)

        Whitebox.setInternalState(cameraPosition, "target", latLng)
        Whitebox.setInternalState(fragment, "editableUploadItem", uploadItem)

        `when`(LocationPicker.getCameraPosition(intent)).thenReturn(cameraPosition)
        `when`(latLng.latitude).thenReturn(0.0)
        `when`(latLng.longitude).thenReturn(0.0)
        `when`(uploadItem.gpsCoords).thenReturn(imageCoordinates)
        fragment.onActivityResult(1211, Activity.RESULT_OK, intent)
        Mockito.verify(presenter, Mockito.times(0)).verifyImageQuality(0, location)
    }

    @Test
    @Throws(Exception::class)
    fun testOnActivityResultAddLocationDialog() {
        shadowOf(Looper.getMainLooper()).idle()
        Mockito.mock(LocationPicker::class.java)
        val intent = Mockito.mock(Intent::class.java)
        val cameraPosition = Mockito.mock(CameraPosition::class.java)
        val latLng = Mockito.mock(LatLng::class.java)

        Whitebox.setInternalState(cameraPosition, "target", latLng)
        Whitebox.setInternalState(fragment, "editableUploadItem", uploadItem)
        Whitebox.setInternalState(fragment,"isMissingLocationDialog",true)
        Whitebox.setInternalState(fragment, "presenter", presenter)

        `when`(LocationPicker.getCameraPosition(intent)).thenReturn(cameraPosition)
        `when`(latLng.latitude).thenReturn(0.0)
        `when`(latLng.longitude).thenReturn(0.0)
        `when`(uploadItem.gpsCoords).thenReturn(imageCoordinates)
        fragment.onActivityResult(1211, Activity.RESULT_OK, intent)
        Mockito.verify(presenter, Mockito.times(1)).verifyImageQuality(0, null)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateMediaDetails() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.updateMediaDetails(null)
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteThisPicture() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = UploadMediaDetailFragment::class.java.getDeclaredMethod(
            "deleteThisPicture"
        )
        method.isAccessible = true
        method.invoke(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroyView() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onDestroyView()
    }

    @Test
    @Throws(Exception::class)
    fun testOnRlContainerTitleClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onRlContainerTitleClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnIbMapClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        fragment.onIbMapClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testOnPrimaryCaptionTextChange() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onPrimaryCaptionTextChange(false)
    }

    @Test
    @Throws(Exception::class)
    fun testOnButtonCopyTitleDescToSubsequentMedia() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "presenter", presenter)
        fragment.onButtonCopyTitleDescToSubsequentMedia()
    }

    @Test
    @Throws(Exception::class)
    fun testDisplayAddLocationDialog() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        runnable = Runnable {  }
        fragment.displayAddLocationDialog(runnable)
    }

    @Test
    @Throws(Exception::class)
    fun testRememberedZoomLevelOnNull(){
        shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(fragment, "defaultKvStore", defaultKvStore)
        `when`(uploadItem.gpsCoords).thenReturn(null)
        `when`(defaultKvStore.getString(LAST_ZOOM)).thenReturn("13.0")
        fragment.showExternalMap(uploadItem)
        Mockito.verify(uploadItem,Mockito.times(1)).gpsCoords
        Mockito.verify(defaultKvStore,Mockito.times(2)).getString(LAST_ZOOM)
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun testRememberedZoomLevelOnNotNull(){
        shadowOf(Looper.getMainLooper()).idle()
        `when`(uploadItem.gpsCoords).thenReturn(imageCoordinates)
        `when`(imageCoordinates.decLatitude).thenReturn(8.0)
        `when`(imageCoordinates.decLongitude).thenReturn(-8.0)
        `when`(imageCoordinates.zoomLevel).thenReturn(14.0)
        `when`(defaultKvStore.getString(LAST_ZOOM)).thenReturn(null)
        fragment.showExternalMap(uploadItem)
        Mockito.verify(uploadItem.gpsCoords,Mockito.times(1)).zoomLevel
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        Assert.assertEquals(shadowIntent.intentClass, LocationPickerActivity::class.java)
    }

}