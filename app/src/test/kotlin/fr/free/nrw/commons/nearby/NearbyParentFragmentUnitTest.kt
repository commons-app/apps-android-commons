package fr.free.nrw.commons.nearby

import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyParentFragmentUnitTest {

    @Mock
    private lateinit var mapView: MapView

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var mapBox: MapboxMap

    @Mock
    private lateinit var presenter: NearbyParentFragmentPresenter

    private lateinit var fragment: NearbyParentFragment

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        fragment = NearbyParentFragment()
        Whitebox.setInternalState(fragment, "mapView", mapView)
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(fragment, "mapBox", mapBox)
        Whitebox.setInternalState(fragment, "presenter", presenter)
    }

    @Test
    @Throws(Exception::class)
    fun `Start map without gps test when last location known`() {
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "startMapWithCondition",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "Without GPS")
        verify(mapView, times(1)).onStart()
        verify(applicationKvStore, times(1)).getString("LastLocation")
        verify(presenter, times(1)).onMapReady()
        val position = CameraPosition.Builder()
            .target(LatLng(
                51.50550,
                -0.07520, 0.0
            ))
            .zoom(0.0)
            .build()
        verify(mapBox, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    @Test
    @Throws(Exception::class)
    fun `Start map without gps test when last location unknown`() {
        `when`(applicationKvStore.getString("LastLocation")).thenReturn("23.76,56.876")
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "startMapWithCondition",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "Without GPS")
        verify(mapView, times(1)).onStart()
        verify(applicationKvStore, times(2)).getString("LastLocation")
        verify(presenter, times(1)).onMapReady()
        val position = CameraPosition.Builder()
            .target(LatLng(
                23.76,
                56.876, 0.0
            ))
            .zoom(14.0)
            .build()
        verify(mapBox, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    @Test
    @Throws(Exception::class)
    fun `Start map without location permission test when last location known`() {
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "startMapWithCondition",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "Without Permission")
        verify(mapView, times(1)).onStart()
        verify(applicationKvStore, times(1)).getString("LastLocation")
        verify(applicationKvStore, times(1))
            .putBoolean("doNotAskForLocationPermission", true)
        verify(presenter, times(1)).onMapReady()
        val position = CameraPosition.Builder()
            .target(LatLng(
                51.50550,
                -0.07520, 0.0
            ))
            .zoom(0.0)
            .build()
        verify(mapBox, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    @Test
    @Throws(Exception::class)
    fun `Start map without location permission test when last location unknown`() {
        `when`(applicationKvStore.getString("LastLocation")).thenReturn("23.76,56.876")
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "startMapWithCondition",
            String::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, "Without Permission")
        verify(mapView, times(1)).onStart()
        verify(applicationKvStore, times(2)).getString("LastLocation")
        verify(applicationKvStore, times(1))
            .putBoolean("doNotAskForLocationPermission", true)
        verify(presenter, times(1)).onMapReady()
        val position = CameraPosition.Builder()
            .target(LatLng(
                23.76,
                56.876, 0.0
            ))
            .zoom(14.0)
            .build()
        verify(mapBox, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }
}