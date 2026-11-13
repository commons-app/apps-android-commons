package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.widget.ImageView
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.location.LocationServiceManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class LocationPickerMapManagerUnitTests {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mapView: MapView

    @Mock
    private lateinit var markerImage: ImageView

    @Mock
    private lateinit var locationManager: LocationServiceManager

    @Mock
    private lateinit var mapController: IMapController

    private lateinit var mapManager: LocationPickerMapManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(mapView.controller).thenReturn(mapController)
        mapManager = LocationPickerMapManager(context, mapView, markerImage, locationManager)
    }

    @Test
    fun moveMapToUpdatesMapCenter() {
        val point = GeoPoint(51.50550, -0.07520)

        mapManager.moveMapTo(point)

        verify(mapController).setCenter(point)
        verify(mapController).animateTo(point)
    }

    @Test
    fun moveMapToMediaLocationUsesCameraPosition() {
        val cameraPosition = CameraPosition(51.50550, -0.07520, 14.0)
        val point = GeoPoint(cameraPosition.latitude, cameraPosition.longitude)

        mapManager.moveMapToMediaLocation(cameraPosition)

        verify(mapController).setCenter(point)
        verify(mapController).animateTo(point)
    }
}
