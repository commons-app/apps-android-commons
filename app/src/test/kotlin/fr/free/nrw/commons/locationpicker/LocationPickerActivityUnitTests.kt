package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.UiSettings
import com.mapbox.mapboxsdk.style.layers.Layer
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_LOCATION
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
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
    private lateinit var mapboxMap: MapboxMap

    @Mock
    private lateinit var cameraPosition: CameraPosition

    @Mock
    private lateinit var modifyLocationButton: Button

    @Mock
    private lateinit var placeSelectedButton: FloatingActionButton

    @Mock
    private lateinit var showInMapButton: TextView

    @Mock
    private lateinit var droppedMarkerLayer: Layer

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

    @Mock
    private lateinit var style: Style

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.getApplication().applicationContext
        activity = Robolectric.buildActivity(LocationPickerActivity::class.java).get()

        Whitebox.setInternalState(activity, "mapboxMap", mapboxMap)
        Whitebox.setInternalState(activity, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(activity, "cameraPosition", cameraPosition)
        Whitebox.setInternalState(activity, "modifyLocationButton", modifyLocationButton)
        Whitebox.setInternalState(activity, "placeSelectedButton", placeSelectedButton)
        Whitebox.setInternalState(activity, "showInMapButton", showInMapButton)
        Whitebox.setInternalState(activity, "droppedMarkerLayer", droppedMarkerLayer)
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
    fun testBindListeners() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "bindListeners"
        )
        method.isAccessible = true
        method.invoke(activity)
        verify(mapboxMap, times(1)).addOnCameraMoveStartedListener(activity)
        verify(mapboxMap, times(1)).addOnCameraIdleListener(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnMapReady() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "onMapReady",
            MapboxMap::class.java
        )
        method.isAccessible = true
        method.invoke(activity, mapboxMap)
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
    fun testAdjustCameraBasedOnOptions() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "adjustCameraBasedOnOptions"
        )
        method.isAccessible = true
        method.invoke(activity)
        verify(mapboxMap, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    @Test
    @Throws(Exception::class)
    fun testOnChanged() {
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "onChanged",
            CameraPosition::class.java
        )
        method.isAccessible = true
        method.invoke(activity, mock(CameraPosition::class.java))
        verify(mapboxMap, times(0)).cameraPosition
    }

    @Test
    @Throws(Exception::class)
    fun testOnStyleLoaded() {
        whenever(modifyLocationButton.visibility).thenReturn(View.INVISIBLE)
        whenever(mapboxMap.uiSettings).thenReturn(mock(UiSettings::class.java))
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "onStyleLoaded",
            Style::class.java
        )
        method.isAccessible = true
        method.invoke(activity, style)
        verify(modifyLocationButton, times(1)).visibility
        verify(mapboxMap, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        verify(mapboxMap, times(1)).uiSettings
        verify(mapboxMap, times(1)).addOnCameraMoveStartedListener(activity)
        verify(mapboxMap, times(1)).addOnCameraIdleListener(activity)
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
        verify(showInMapButton, times(1)).visibility = View.GONE
        verify(markerImage, times(1)).visibility = View.VISIBLE
        verify(shadow, times(1)).visibility = View.VISIBLE
        verify(droppedMarkerLayer, times(1)).setProperties(any())
        verify(largeToolbarText, times(1)).text = "Choose a location"
        verify(smallToolbarText, times(1)).text = "Pan and zoom to adjust"
        verify(fabCenterOnLocation, times(1)).visibility = View.VISIBLE
        verify(mapboxMap, times(1)).addOnCameraMoveStartedListener(activity)
        verify(mapboxMap, times(1)).addOnCameraIdleListener(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testPlaceSelected() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(activity,"activity", "NoLocationUploadActivity")
        val position = CameraPosition.Builder().target(
            LatLng(
                51.50550,
                -0.07520,
                0.0
            )
        ).zoom(15.0).build()
        `when`(mapboxMap.cameraPosition).thenReturn(position)
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "placeSelected"
        )
        method.isAccessible = true
        method.invoke(activity)
        verify(applicationKvStore, times(1))
            .putString(LAST_LOCATION, position.target.latitude.toString()
                    + ","
                    + position.target.longitude
            )
        verify(applicationKvStore, times(1))
            .putString(LAST_ZOOM, position.zoom.toString())
        verify(mapboxMap, times(4)).cameraPosition
    }

}