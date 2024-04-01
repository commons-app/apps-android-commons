package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_LOCATION
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.osmdroid.util.GeoPoint
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LocationPickerActivityUnitTests {

    private lateinit var activity: LocationPickerActivity
    private lateinit var context: Context

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var mapView: org.osmdroid.views.MapView

    @Mock
    private lateinit var cameraPosition: CameraPosition

    @Mock
    private lateinit var modifyLocationButton: Button

    @Mock
    private lateinit var removeLocationButton: Button

    @Mock
    private lateinit var placeSelectedButton: FloatingActionButton

    @Mock
    private lateinit var showInMapButton: TextView

    @Mock
    private lateinit var markerImage: ImageView

    @Mock
    private lateinit var shadow: ImageView

    @Mock
    private lateinit var largeToolbarText: TextView

    @Mock
    private lateinit var smallToolbarText: TextView

    @Mock
    private lateinit var fabCenterOnLocation: FloatingActionButton

    @Mock
    private lateinit var tvAttribution: AppCompatTextView

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.getApplication().applicationContext
        activity = Robolectric.buildActivity(LocationPickerActivity::class.java).get()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }

        Whitebox.setInternalState(activity, "mapView", mapView)
        Whitebox.setInternalState(activity, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(activity, "cameraPosition", cameraPosition)
        Whitebox.setInternalState(activity, "modifyLocationButton", modifyLocationButton)
        Whitebox.setInternalState(activity, "removeLocationButton", removeLocationButton)
        Whitebox.setInternalState(activity, "placeSelectedButton", placeSelectedButton)
        Whitebox.setInternalState(activity, "showInMapButton", showInMapButton)
        Whitebox.setInternalState(activity, "markerImage", markerImage)
        Whitebox.setInternalState(activity, "shadow", shadow)
        Whitebox.setInternalState(activity, "largeToolbarText", largeToolbarText)
        Whitebox.setInternalState(activity, "smallToolbarText", smallToolbarText)
        Whitebox.setInternalState(activity, "fabCenterOnLocation", fabCenterOnLocation)
        Whitebox.setInternalState(activity, "tvAttribution", tvAttribution)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testAddCredits() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "addCredits"
        )
        method.isAccessible = true
        method.invoke(activity)
        verify(tvAttribution).text = any()
        verify(tvAttribution).movementMethod = any()
    }

    @Test
    @Throws(Exception::class)
    fun testOnClickModifyLocation() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "onClickModifyLocation"
        )
        method.isAccessible = true
        method.invoke(activity)
        verify(placeSelectedButton, times(1)).visibility = View.VISIBLE
        verify(modifyLocationButton, times(1)).visibility = View.GONE
        verify(removeLocationButton, times(1)).visibility = View.GONE
        verify(showInMapButton, times(1)).visibility = View.GONE
        verify(markerImage, times(1)).visibility = View.VISIBLE
        verify(shadow, times(1)).visibility = View.VISIBLE
        verify(largeToolbarText, times(1)).text = "Choose a location"
        verify(smallToolbarText, times(1)).text = "Pan and zoom to adjust"
        verify(fabCenterOnLocation, times(1)).visibility = View.VISIBLE
    }

    @Test
    @Throws(Exception::class)
    fun testOnClickRemoveLocation() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "onClickRemoveLocation"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testPlaceSelected() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(activity,"activity", "NoLocationUploadActivity")
        val position = GeoPoint(51.50550, -0.07520)
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "placeSelected"
        )
        `when`(mapView.mapCenter).thenReturn(position)
        `when`(mapView.zoomLevel).thenReturn(15)
        method.isAccessible = true
        method.invoke(activity)
        verify(applicationKvStore, times(1)).putString(
            LAST_LOCATION,
            position.latitude.toString() + "," + position.longitude.toString()
        )
        verify(applicationKvStore, times(1)).putString(LAST_ZOOM, mapView.zoomLevel.toString())
    }



}
