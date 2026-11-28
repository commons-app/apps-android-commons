package fr.free.nrw.commons.nearby

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.ContributionUploadCoordinator
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.wikidata.WikidataEditListener
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowToast
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
    private lateinit var presenter: NearbyParentFragmentPresenter

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var configuration: Configuration

    @Mock
    private lateinit var rlBottomSheet: RelativeLayout

    @Mock
    private lateinit var rlBottomSheetLayoutParams: ViewGroup.LayoutParams

    @Mock
    private lateinit var nearbyParentFragmentInstanceReadyCallback:
        NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback

    @Mock
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @Mock
    private lateinit var locationManager: LocationServiceManager

    @Mock
    private lateinit var wikidataEditListener: WikidataEditListener

    @Mock
    private lateinit var fab: FloatingActionButton

    @Mock
    private lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Mock
    private lateinit var uploadCoordinator: ContributionUploadCoordinator

    private lateinit var layoutInflater: LayoutInflater
    private lateinit var fragment: NearbyParentFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        OkHttpConnectionFactory.CLIENT = createTestClient()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        fragment = NearbyParentFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(fragment, "presenter", presenter)
        Whitebox.setInternalState(fragment, "isVisibleToUser", true)
        Whitebox.setInternalState(fragment, "bottomSheetListBehavior", bottomSheetBehavior)
        Whitebox.setInternalState(fragment, "bottomSheetDetailsBehavior", bottomSheetBehavior)
        Whitebox.setInternalState(fragment, "locationManager", locationManager)
        Whitebox.setInternalState(fragment, "wikidataEditListener", wikidataEditListener)
        Whitebox.setInternalState(fragment, "bookmarkLocationDao", bookmarkLocationDao)
        Whitebox.setInternalState(fragment, "uploadCoordinator", uploadCoordinator)

        Whitebox.setInternalState(
            fragment,
            "nearbyParentFragmentInstanceReadyCallback",
            nearbyParentFragmentInstanceReadyCallback,
        )
    }

    @Test
    fun `reset advanced query reloads the default search and keeps the editor open`() {
        fragment.resetAdvancedQuery()
        verify(presenter).setAdvancedQuery("")
        verify(presenter).updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun `Start map without gps test when last location known`() {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "startMapWithCondition",
                String::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, "Without GPS")
        verify(applicationKvStore, times(1)).getString("LastLocation")
        verify(presenter, times(1)).onMapReady()
        val position =
            GeoPoint(
                51.50550,
                -0.07520,
            )
        verify(mapView, times(1))
            .controller
            .animateTo(position)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun `Start map without gps test when last location unknown`() {
        `when`(applicationKvStore.getString("LastLocation")).thenReturn("23.76,56.876")
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "startMapWithCondition",
                String::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, "Without GPS")
        verify(applicationKvStore, times(2)).getString("LastLocation")
        verify(presenter, times(1)).onMapReady()
        val position = GeoPoint(23.76, 56.876)
        verify(mapView, times(1))
            .controller
            .animateTo(position)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun `Start map without location permission test when last location known`() {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "startMapWithCondition",
                String::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, "Without Permission")
        verify(applicationKvStore, times(1)).getString("LastLocation")
        verify(applicationKvStore, times(1))
            .putBoolean("doNotAskForLocationPermission", true)
        verify(presenter, times(1)).onMapReady()
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun `Start map without location permission test when last location unknown`() {
        `when`(applicationKvStore.getString("LastLocation")).thenReturn("23.76,56.876")
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "startMapWithCondition",
                String::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, "Without Permission")
        verify(applicationKvStore, times(2)).getString("LastLocation")
        verify(applicationKvStore, times(1))
            .putBoolean("doNotAskForLocationPermission", true)
        verify(presenter, times(1)).onMapReady()
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testOnLearnMoreClicked() {
        fragment.onLearnMoreClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.`data`, Uri.parse(NearbyParentFragment.WLM_URL))
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testOnConfigurationChanged() {
        `when`(rlBottomSheet.layoutParams).thenReturn(rlBottomSheetLayoutParams)
        fragment.onConfigurationChanged(configuration)
        verify(rlBottomSheet).layoutParams
        verify(rlBottomSheet).layoutParams = rlBottomSheetLayoutParams
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testSetNearbyParentFragmentInstanceReadyCallback() {
        fragment.setNearbyParentFragmentInstanceReadyCallback(
            nearbyParentFragmentInstanceReadyCallback,
        )
        Assert.assertEquals(
            nearbyParentFragmentInstanceReadyCallback,
            nearbyParentFragmentInstanceReadyCallback,
        )
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testSetUserVisibleHintCaseFalse() {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "setUserVisibleHint",
                Boolean::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, false)
        verify(bottomSheetBehavior, times(2)).state = BottomSheetBehavior.STATE_HIDDEN
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testSetUserVisibleHintCaseTrue() {
        Whitebox.setInternalState(fragment, "mState", 4)
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "setUserVisibleHint",
                Boolean::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testRegisterUnregisterLocationListenerCaseTrue() {
        fragment.registerUnregisterLocationListener(true)
        verify(locationManager).unregisterLocationManager()
        verify(locationManager).removeLocationListener(fragment)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testRegisterUnregisterLocationListenerCaseFalse() {
        fragment.registerUnregisterLocationListener(false)
        verify(locationManager).addLocationListener(fragment)
        verify(locationManager).registerLocationManager()
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testOnWikidataEditSuccessful() {
        fragment.onWikidataEditSuccessful()
        verify(presenter).updateMapAndList(LocationChangeType.MAP_UPDATED)
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testOnDestroy() {
        fragment.onDestroy()
        verify(wikidataEditListener).authenticationStateListener = null
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testPrepareViewsForSheetPositionCaseCollapsed() {
        Whitebox.setInternalState(fragment, "isFABsExpanded", true)
        Whitebox.setInternalState(fragment, "mView", view)
        whenever(view.findViewById(R.id.empty_view) as View?).thenReturn(view)
        whenever(view.findViewById(R.id.empty_view1) as View?).thenReturn(view)
        whenever(view.id).thenReturn(0)
        fragment.prepareViewsForSheetPosition(BottomSheetBehavior.STATE_COLLAPSED)
        verify(fab).isShown
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testPrepareViewsForSheetPositionCaseHidden() {
        Whitebox.setInternalState(fragment, "isFABsExpanded", true)
        Whitebox.setInternalState(fragment, "mView", view)
        whenever(view.findViewById(R.id.empty_view) as View?).thenReturn(view)
        whenever(view.findViewById(R.id.empty_view1) as View?).thenReturn(view)
        whenever(view.id).thenReturn(0)
        whenever(fab.layoutParams).thenReturn(mock(CoordinatorLayout.LayoutParams::class.java))
        fragment.prepareViewsForSheetPosition(BottomSheetBehavior.STATE_HIDDEN)
        verify(fab, times(5)).hide()
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testDisplayBottomSheetWithInfo() {
        val nearbyBaseMarker = mock(BaseMarker::class.java)
        val place = mock(Place::class.java)
        val label = mock(Label::class.java)
        whenever(nearbyBaseMarker.place).thenReturn(place)
        whenever(place.label).thenReturn(label)
        whenever(place.longDescription).thenReturn("")
        verify(bottomSheetBehavior).state = BottomSheetBehavior.STATE_COLLAPSED
    }

    @Test @Ignore
    @Throws(Exception::class)
    fun testOpenLocationSettingsCaseNull() {
        fragment.openLocationSettings()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        Assert.assertEquals(shadowActivity.nextStartedActivityForResult, null)
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testDepictsPickerInstanceSetsArguments() {
        val allPhotoCoordinates = ArrayList<LatLng>()
        val pickerLat = 12.34
        val pickerLng = 56.78
        allPhotoCoordinates.add(
            LatLng(
                pickerLat,
                pickerLng,
                accuracy = 0f,
            ),
        )
        val pickerFragment =
            NearbyParentFragment.newPickerInstance(allPhotoCoordinates)
        Assert.assertNotNull(pickerFragment.arguments)
        Assert.assertTrue(
            pickerFragment.arguments!!.getBoolean(NearbyParentFragment.ARG_PICKER_MODE),
        )
        Assert.assertEquals(
            allPhotoCoordinates,
            pickerFragment.arguments!!.getParcelableArrayList<LatLng>(
                NearbyParentFragment.ARG_PHOTO_LATLNGS,
            ),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testMarkerPriorityOrder() {
        // Use reflection to access the private getMarkerPriority method
        val getMarkerPriorityMethod =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "getMarkerPriority",
                Place::class.java,
                Boolean::class.java,
            )
        getMarkerPriorityMethod.isAccessible = true

        // Mock a place that needs a photo (Red pin)
        val needsPhotoPlace = mock(Place::class.java)
        `when`(needsPhotoPlace.isMonument).thenReturn(false)
        needsPhotoPlace.pic = ""
        needsPhotoPlace.exists = true
        needsPhotoPlace.name = "Needs Photo Place"

        // Mock a place that has a photo (Green pin)
        val hasPhotoPlace = mock(Place::class.java)
        `when`(hasPhotoPlace.isMonument).thenReturn(false)
        hasPhotoPlace.pic = "some_pic"
        hasPhotoPlace.exists = true
        hasPhotoPlace.name = "Has Photo Place"

        // Mock a monument place
        val monumentPlace = mock(Place::class.java)
        `when`(monumentPlace.isMonument).thenReturn(true)
        monumentPlace.pic = ""
        monumentPlace.exists = true
        monumentPlace.name = "Monument Place"

        // Get priorities
        val priorityNeedsPhoto = getMarkerPriorityMethod.invoke(fragment, needsPhotoPlace, false) as Int
        val priorityHasPhoto = getMarkerPriorityMethod.invoke(fragment, hasPhotoPlace, false) as Int
        val priorityMonument = getMarkerPriorityMethod.invoke(fragment, monumentPlace, false) as Int

        // Red (needs photo) should be drawn on top of Green (has photo), so Red > Green priority
        Assert.assertTrue(
            "Needs Photo (Red) priority ($priorityNeedsPhoto) should be greater than " +
                "Has Photo (Green) priority ($priorityHasPhoto)",
            priorityNeedsPhoto > priorityHasPhoto,
        )

        // Red should also be drawn on top of Monuments
        Assert.assertTrue(
            "Needs Photo (Red) priority ($priorityNeedsPhoto) should be greater than " +
                "Monument priority ($priorityMonument)",
            priorityNeedsPhoto > priorityMonument,
        )
    }

    @Test
    fun `onDestroyView keeps nearby place while picker result is pending`() {
        whenever(uploadCoordinator.isInternalUploadInProgress).thenReturn(true)

        fragment.onDestroyView()

        verify(presenter, never()).removeNearbyPreferences(applicationKvStore)
    }

    @Test
    fun `onDestroyView clears nearby place when no picker result is pending`() {
        fragment.onDestroyView()

        verify(presenter).removeNearbyPreferences(applicationKvStore)
    }

    @Test
    fun `openFile with GPX uses standard application gpx xml MIME type`() {
        invokeOpenFile(activity, "nearby.gpx", isGPX = true)

        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        Assert.assertNotNull(startedIntent)
        Assert.assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        Assert.assertEquals("application/gpx+xml", startedIntent.type)
        Assert.assertNotNull(startedIntent.data)
        Assert.assertTrue((startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    @Test
    fun `openFile with KML uses standard application vnd google earth kml xml MIME type`() {
        invokeOpenFile(activity, "nearby.kml", isGPX = false)

        val startedIntent = Shadows.shadowOf(activity).nextStartedActivity
        Assert.assertNotNull(startedIntent)
        Assert.assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        Assert.assertEquals("application/vnd.google-earth.kml+xml", startedIntent.type)
        Assert.assertNotNull(startedIntent.data)
        Assert.assertTrue((startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }

    @Test
    fun `openFile shows GPX error toast when no app can handle GPX intent`() {
        val throwingContext = createThrowingContext("No GPX viewer")

        invokeOpenFile(throwingContext, "nearby.gpx", isGPX = true)

        Assert.assertEquals(
            activity.getString(R.string.no_application_available_to_open_gpx_files),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    @Test
    fun `openFile shows KML error toast when no app can handle KML intent`() {
        val throwingContext = createThrowingContext("No KML viewer")

        invokeOpenFile(throwingContext, "nearby.kml", isGPX = false)

        Assert.assertEquals(
            activity.getString(R.string.no_application_available_to_open_kml_files),
            ShadowToast.getTextOfLatestToast(),
        )
    }

    private fun createThrowingContext(errorMessage: String): Context =
        object : ContextWrapper(activity) {
            override fun startActivity(intent: Intent?) = throw ActivityNotFoundException(errorMessage)
        }

    private fun invokeOpenFile(
        targetContext: Context,
        fileName: String,
        isGPX: Boolean,
    ) {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "openFile",
                Context::class.java,
                String::class.java,
                Boolean::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, targetContext, fileName, isGPX)
    }

    @Test
    fun `locationPermissionGranted with shouldRecenter true centers map and marks centered`() {
        val mapController = setupMapForRecenterTest()
        val gpsLatLng = LatLng(TEST_GPS_LAT, TEST_GPS_LNG, TEST_ACCURACY)
        whenever(locationManager.getLastLocation()).thenReturn(gpsLatLng)

        invokeLocationPermissionGranted(shouldRecenter = true)

        val expectedPoint = GeoPoint(TEST_GPS_LAT, TEST_GPS_LNG)
        verify(mapController).setCenter(expectedPoint)
        verify(mapController).animateTo(expectedPoint)
        verify(presenter).onMapReady()
        Assert.assertEquals(expectedPoint, Whitebox.getInternalState<GeoPoint>(fragment, "mapCenter"))
        Assert.assertTrue(Whitebox.getInternalState<Boolean>(fragment, "hasCenteredMapOnLocation"))
    }

    @Test
    fun `locationPermissionGranted with shouldRecenter false preserves mapCenter and skips camera recenter`() {
        val mapController = setupMapForRecenterTest()
        val pannedPoint = GeoPoint(TEST_PANNED_LAT, TEST_PANNED_LNG)
        Whitebox.setInternalState(fragment, "mapCenter", pannedPoint)
        Whitebox.setInternalState(fragment, "hasCenteredMapOnLocation", true)
        whenever(locationManager.getLastLocation()).thenReturn(LatLng(TEST_GPS_LAT, TEST_GPS_LNG, TEST_ACCURACY))

        invokeLocationPermissionGranted(shouldRecenter = false)

        verify(mapController, never()).setCenter(any())
        verify(mapController, never()).animateTo(any())
        verify(presenter).onMapReady()
        Assert.assertEquals(pannedPoint, Whitebox.getInternalState<GeoPoint>(fragment, "mapCenter"))
    }

    @Test
    fun `locationPermissionGranted with null lastKnownLocation does not mark map as centered`() {
        setupMapForRecenterTest()
        whenever(locationManager.getLastLocation()).thenReturn(null)
        whenever(locationManager.isGPSProviderEnabled()).thenReturn(true)

        invokeLocationPermissionGranted(shouldRecenter = true)

        Assert.assertFalse(Whitebox.getInternalState<Boolean>(fragment, "hasCenteredMapOnLocation"))
        verify(presenter).onMapReady()
    }

    @Test
    fun `startMapWithoutPermission with shouldRecenter false preserves camera position`() {
        val mapController = setupMapForRecenterTest()
        Whitebox.setInternalState(fragment, "hasCenteredMapOnLocation", true)
        whenever(applicationKvStore.getString("LastLocation")).thenReturn("$TEST_GPS_LAT,$TEST_GPS_LNG")

        invokeStartMapWithoutPermission(shouldRecenter = false)

        verify(mapController, never()).animateTo(any())
        verify(presenter).onMapReady()
    }

    @Test
    fun `startMapWithoutPermission with shouldRecenter true moves camera and marks centered`() {
        val mapController = setupMapForRecenterTest()
        whenever(applicationKvStore.getString("LastLocation")).thenReturn("$TEST_GPS_LAT,$TEST_GPS_LNG")

        invokeStartMapWithoutPermission(shouldRecenter = true)

        verify(mapController).animateTo(GeoPoint(TEST_GPS_LAT, TEST_GPS_LNG))
        verify(presenter).onMapReady()
        Assert.assertTrue(Whitebox.getInternalState<Boolean>(fragment, "hasCenteredMapOnLocation"))
    }

    private fun setupMapForRecenterTest(): IMapController {
        val mapController = mock(IMapController::class.java)
        val binding = mock(FragmentNearbyParentBinding::class.java)
        Whitebox.setInternalState(binding, "map", mapView)
        Whitebox.setInternalState(binding, "mapProgressBar", mock(ProgressBar::class.java))
        Whitebox.setInternalState(fragment, "binding", binding)
        whenever(mapView.controller).thenReturn(mapController)
        whenever(mapView.overlays).thenReturn(null)
        return mapController
    }

    private fun invokeLocationPermissionGranted(shouldRecenter: Boolean) {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "locationPermissionGranted",
                Boolean::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, shouldRecenter)
    }

    private fun invokeStartMapWithoutPermission(shouldRecenter: Boolean) {
        val method: Method =
            NearbyParentFragment::class.java.getDeclaredMethod(
                "startMapWithoutPermission",
                Boolean::class.java,
            )
        method.isAccessible = true
        method.invoke(fragment, shouldRecenter)
    }

    companion object {
        private const val TEST_GPS_LAT = 35.6762
        private const val TEST_GPS_LNG = 139.6503
        private const val TEST_PANNED_LAT = 48.8566
        private const val TEST_PANNED_LNG = 2.3522
        private const val TEST_ACCURACY = 1f
    }
}
