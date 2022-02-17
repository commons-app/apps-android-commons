package fr.free.nrw.commons.locationpicker

import android.os.Looper
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import fr.free.nrw.commons.LocationPicker.LocationPickerActivity
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.LAST_ZOOM
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LocationPickerActivityTest {

    private lateinit var activityLocationPicker : LocationPickerActivity

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var mapboxMap : MapboxMap


    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
        activityLocationPicker = Robolectric.buildActivity(LocationPickerActivity::class.java).get()
    }

    @Test
    @Throws(Exception::class)
    fun testRememberZoomLevel(){
        shadowOf(Looper.getMainLooper()).idle()
        Whitebox.setInternalState(activityLocationPicker,"mapboxMap",mapboxMap)
        Whitebox.setInternalState(activityLocationPicker,"activity", "NoLocationUploadActivity")
        Whitebox.setInternalState(activityLocationPicker,"applicationKvStore",applicationKvStore)
        val position = CameraPosition.Builder().target(LatLng(
                51.50550,
                -0.07520, 0.0
            )
        ).zoom(15.0).build()
        `when`(mapboxMap.cameraPosition).thenReturn(position)
        val method: Method = LocationPickerActivity::class.java.getDeclaredMethod(
            "placeSelected"
        )
        method.isAccessible = true
        method.invoke(activityLocationPicker)
        Mockito.verify(applicationKvStore,Mockito.times(1))
            .putString(LAST_ZOOM,position.zoom.toString())
    }
}