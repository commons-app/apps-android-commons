package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.widget.ImageView
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
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

    @Test
    fun startLocationForImageWithoutLocationUsesLastPickedLocation() {
        val lastPicked = CameraPosition(35.683, 139.76, 14.0)
        `when`(locationManager.getLastLocation()).thenReturn(LatLng(51.5055, -0.0752, 0f))

        mapManager.moveMapToStartLocation("NoLocationUploadActivity", lastPicked)

        verify(mapController).setCenter(GeoPoint(35.683, 139.76))
        verify(locationManager, never()).getLastLocation()
    }

    @Test
    fun startLocationForImageWithoutLocationFallsBackToGpsWhenNothingWasPicked() {
        `when`(locationManager.getLastLocation()).thenReturn(LatLng(51.5055, -0.0752, 0f))

        mapManager.moveMapToStartLocation("NoLocationUploadActivity", null)

        verify(mapController).setCenter(GeoPoint(51.5055, -0.0752))
    }

    @Test
    fun startLocationForImageWithLocationUsesImageLocation() {
        val imageLocation = CameraPosition(48.8584, 2.2945, 16.0)

        mapManager.moveMapToStartLocation("UploadActivity", imageLocation)

        verify(mapController).setCenter(GeoPoint(48.8584, 2.2945))
        verify(locationManager, never()).getLastLocation()
    }

    @Test
    fun startLocationForImageWithLocationDoesNotMoveWithoutPosition() {
        mapManager.moveMapToStartLocation("UploadActivity", null)

        verifyNoInteractions(mapController)
        verify(locationManager, never()).getLastLocation()
    }

    @Test
    fun startLocationForMediaDetailsUsesMediaLocation() {
        val mediaLocation = CameraPosition(40.6892, -74.0445, 15.0)

        mapManager.moveMapToStartLocation("MediaActivity", mediaLocation)

        verify(mapController).setCenter(GeoPoint(40.6892, -74.0445))
        verify(locationManager, never()).getLastLocation()
    }

    @Test
    fun startLocationWithoutSourceUsesGps() {
        `when`(locationManager.getLastLocation()).thenReturn(LatLng(51.5055, -0.0752, 0f))

        mapManager.moveMapToStartLocation(null, CameraPosition(40.6892, -74.0445, 15.0))

        verify(mapController).setCenter(GeoPoint(51.5055, -0.0752))
    }
}
