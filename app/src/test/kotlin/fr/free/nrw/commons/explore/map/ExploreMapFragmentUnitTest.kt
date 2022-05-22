package fr.free.nrw.commons.explore.map

import android.content.Context
import android.os.Looper.getMainLooper
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.PlacesInfo
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.explore.ExploreFragment
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.utils.PermissionUtils
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
class ExploreMapFragmentUnitTest {

    @Mock
    private lateinit var mapView: MapView

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var mapBox: MapboxMap

    @Mock
    private lateinit var presenter: ExploreMapPresenter

    @Mock
    private lateinit var projectorLatLngBounds: LatLngBounds

    private lateinit var parentFragment: ExploreFragment
    private lateinit var fragment: ExploreMapFragment
    private lateinit var context: Context
    private lateinit var activity: MainActivity
    private lateinit var fragmentManager: FragmentManager
    private lateinit var layoutInflater: LayoutInflater

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        AppAdapter.set(TestAppAdapter())
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        fragment = ExploreMapFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()
        layoutInflater = LayoutInflater.from(activity)

        Whitebox.setInternalState(fragment, "presenter", presenter)
    }

    @Test
    fun testPopulatePlacesCurLatLngNull() {
        fragment.populatePlaces(null, null)
        verifyZeroInteractions(presenter)
    }

    @Test
    fun testPopulatePlacesCheckingAroundCurrentLocation() {
        fragment.populatePlaces(LatLng(0.0,0.0,0.0f), LatLng(10.0,10.0,0.0f))
        verify(presenter).loadAttractionsFromLocation(LatLng(0.0,0.0,0.0f), LatLng(10.0,10.0,0.0f), true)
    }

    @Test
    fun testRecenterMapWhenCurLatLngNull() {
        fragment.recenterMap(null)
        verifyZeroInteractions(presenter)
        verifyZeroInteractions(mapBox)
        verifyZeroInteractions(mapView)
    }

    /*@Test
    fun testRecenterMapWhenCurLatLngPermissionDenied() {
        fragment.recenterMap(LatLng(0.0,0.0,0.0f))
        verifyZeroInteractions(presenter)
        verifyZeroInteractions(mapBox)
        verifyZeroInteractions(mapView)
    } */

    fun provideLocationPermission() {
        /*Mockito.mockStatic(PermissionUtils::class.java).use { mockedUuid ->
            mockedUuid.`when`<Any> { PermissionUtils.checkPermissionsAndPerformAction(any(), any(), any(), any(), any(), any()) }
                .thenReturn(defaultUuid)*/
    }

    fun denyLocationPermission() {

    }
}