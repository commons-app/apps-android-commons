package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.os.Looper
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
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

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        activity = Robolectric.buildActivity(LocationPickerActivity::class.java).get()

        Whitebox.setInternalState(activity, "mapboxMap", mapboxMap)
        Whitebox.setInternalState(activity, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(activity, "cameraPosition", cameraPosition)
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