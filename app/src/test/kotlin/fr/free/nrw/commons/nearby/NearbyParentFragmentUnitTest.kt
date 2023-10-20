package fr.free.nrw.commons.nearby

import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.wikidata.WikidataEditListener
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowAlertDialog
import org.wikipedia.AppAdapter
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

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var ivToggleChips: AppCompatImageView

    @Mock
    private lateinit var configuration: Configuration

    @Mock
    private lateinit var rlBottomSheet: RelativeLayout

    @Mock
    private lateinit var rlBottomSheetLayoutParams: ViewGroup.LayoutParams

    @Mock
    private lateinit var nearbyParentFragmentInstanceReadyCallback: NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback

    @Mock
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    @Mock
    private lateinit var locationManager: LocationServiceManager

    @Mock
    private lateinit var wikidataEditListener: WikidataEditListener

    @Mock
    private lateinit var fab: FloatingActionButton

    @Mock
    private lateinit var bottomSheetDetails: View

    @Mock
    private lateinit var marker: NearbyMarker

    @Mock
    private lateinit var linearLayout: LinearLayout

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var bookmarkLocationDao: BookmarkLocationsDao

    private lateinit var layoutInflater: LayoutInflater
    private lateinit var fragment: NearbyParentFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        AppAdapter.set(TestAppAdapter())
        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()

        fragment = NearbyParentFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        layoutInflater = LayoutInflater.from(activity)

        Whitebox.setInternalState(fragment, "mapView", mapView)
        Whitebox.setInternalState(fragment, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(fragment, "mapBox", mapBox)
        Whitebox.setInternalState(fragment, "presenter", presenter)
        Whitebox.setInternalState(fragment, "llContainerChips", view)
        Whitebox.setInternalState(fragment, "ivToggleChips", ivToggleChips)
        Whitebox.setInternalState(fragment, "rlBottomSheet", rlBottomSheet)
        Whitebox.setInternalState(fragment, "isVisibleToUser", true)
        Whitebox.setInternalState(fragment, "bottomSheetListBehavior", bottomSheetBehavior)
        Whitebox.setInternalState(fragment, "bottomSheetDetailsBehavior", bottomSheetBehavior)
        Whitebox.setInternalState(fragment, "locationManager", locationManager)
        Whitebox.setInternalState(fragment, "wikidataEditListener", wikidataEditListener)
        Whitebox.setInternalState(fragment, "fabPlus", fab)
        Whitebox.setInternalState(fragment, "fabCamera", fab)
        Whitebox.setInternalState(fragment, "fabGallery", fab)
        Whitebox.setInternalState(fragment, "fabGallery", fab)
        Whitebox.setInternalState(fragment, "bottomSheetDetails", bottomSheetDetails)
        Whitebox.setInternalState(fragment, "transparentView", view)
        Whitebox.setInternalState(fragment, "bookmarkButton", linearLayout)
        Whitebox.setInternalState(fragment, "wikipediaButton", linearLayout)
        Whitebox.setInternalState(fragment, "wikidataButton", linearLayout)
        Whitebox.setInternalState(fragment, "directionsButton", linearLayout)
        Whitebox.setInternalState(fragment, "commonsButton", linearLayout)
        Whitebox.setInternalState(fragment, "bookmarkLocationDao", bookmarkLocationDao)

        Whitebox.setInternalState(fragment, "icon", imageView)
        Whitebox.setInternalState(fragment, "title", textView)
        Whitebox.setInternalState(fragment, "distance", textView)
        Whitebox.setInternalState(fragment, "description", textView)

        Whitebox.setInternalState(
            fragment,
            "nearbyParentFragmentInstanceReadyCallback",
            nearbyParentFragmentInstanceReadyCallback
        )
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
            .target(
                LatLng(
                    51.50550,
                    -0.07520, 0.0
                )
            )
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
            .target(
                LatLng(
                    23.76,
                    56.876, 0.0
                )
            )
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
            .target(
                LatLng(
                    51.50550,
                    -0.07520, 0.0
                )
            )
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
            .target(
                LatLng(
                    23.76,
                    56.876, 0.0
                )
            )
            .zoom(14.0)
            .build()
        verify(mapBox, times(1))
            .moveCamera(CameraUpdateFactory.newCameraPosition(position))
    }

    @Test
    @Throws(Exception::class)
    fun testOnToggleChipsClickedCaseVisible() {
        `when`(view.visibility).thenReturn(View.VISIBLE)
        fragment.onToggleChipsClicked()
        verify(view).visibility = View.GONE
        verify(ivToggleChips).rotation = ivToggleChips.rotation + 180
    }

    @Test
    @Throws(Exception::class)
    fun testOnToggleChipsClickedCaseNotVisible() {
        `when`(view.visibility).thenReturn(View.GONE)
        fragment.onToggleChipsClicked()
        verify(view).visibility = View.VISIBLE
        verify(ivToggleChips).rotation = ivToggleChips.rotation + 180
    }

    @Test
    @Throws(Exception::class)
    fun testOnLearnMoreClicked() {
        fragment.onLearnMoreClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.`data`, Uri.parse(NearbyParentFragment.WLM_URL))
    }

    @Test
    @Throws(Exception::class)
    fun testOnConfigurationChanged() {
        `when`(rlBottomSheet.layoutParams).thenReturn(rlBottomSheetLayoutParams)
        fragment.onConfigurationChanged(configuration)
        verify(rlBottomSheet).layoutParams
        verify(rlBottomSheet).layoutParams = rlBottomSheetLayoutParams
    }

    @Test
    @Throws(Exception::class)
    fun testSetNearbyParentFragmentInstanceReadyCallback() {
        fragment.setNearbyParentFragmentInstanceReadyCallback(
            nearbyParentFragmentInstanceReadyCallback
        )
        Assert.assertEquals(
            nearbyParentFragmentInstanceReadyCallback,
            nearbyParentFragmentInstanceReadyCallback
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserVisibleHintCaseFalse() {
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "setUserVisibleHint", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, false)
        verify(bottomSheetBehavior, times(2)).state = BottomSheetBehavior.STATE_HIDDEN
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserVisibleHintCaseTrue() {
        Whitebox.setInternalState(fragment, "mState", 4)
        val method: Method = NearbyParentFragment::class.java.getDeclaredMethod(
            "setUserVisibleHint", Boolean::class.java
        )
        method.isAccessible = true
        method.invoke(fragment, true)
    }

    @Test
    @Throws(Exception::class)
    fun testRegisterUnregisterLocationListenerCaseTrue() {
        fragment.registerUnregisterLocationListener(true)
        verify(locationManager).unregisterLocationManager()
        verify(locationManager).removeLocationListener(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testRegisterUnregisterLocationListenerCaseFalse() {
        fragment.registerUnregisterLocationListener(false)
        verify(locationManager).addLocationListener(fragment)
        verify(locationManager).registerLocationManager()
    }

    @Test
    @Throws(Exception::class)
    fun testOnWikidataEditSuccessful() {
        fragment.onWikidataEditSuccessful()
        verify(presenter).updateMapAndList(LocationChangeType.MAP_UPDATED)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        fragment.onDestroy()
        verify(wikidataEditListener).setAuthenticationStateListener(null)
    }

    @Test
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

    @Test
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

    @Test
    @Throws(Exception::class)
    fun testDisplayBottomSheetWithInfo() {
        val nearbyBaseMarker = mock(NearbyBaseMarker::class.java)
        val place = mock(Place::class.java)
        val label = mock(Label::class.java)
        whenever(marker.nearbyBaseMarker).thenReturn(nearbyBaseMarker)
        whenever(nearbyBaseMarker.place).thenReturn(place)
        whenever(place.label).thenReturn(label)
        whenever(place.longDescription).thenReturn("")
        fragment.displayBottomSheetWithInfo(marker)
        verify(bottomSheetBehavior).state = BottomSheetBehavior.STATE_COLLAPSED
    }

    @Test
    @Throws(Exception::class)
    fun testOpenLocationSettingsCaseNull() {
        fragment.openLocationSettings()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        Assert.assertEquals(shadowActivity.nextStartedActivityForResult, null)
    }

    @Test
    @Throws(Exception::class)
    fun testShowLocationOffDialog() {
        fragment.showLocationOffDialog()
        val dialog: AlertDialog = ShadowAlertDialog.getLatestDialog() as AlertDialog
        Assert.assertEquals(dialog.isShowing, true)
    }

}