package fr.free.nrw.commons.explore.map

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.contributions.loadNearbyMapFromExplore
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.showLongSnackbar
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment that displays the Wikimedia Commons Explore map interface, managing
 * place markers, location tracking, camera viewports, and place detail sheets.
 */
class ExploreMapFragment(
    val detailsSheetHelper: ExploreMapDetailsSheetHelper = ExploreMapDetailsSheetHelper(),
    val markerOverlayHelper: ExploreMapMarkerOverlayHelper = ExploreMapMarkerOverlayHelper(),
    val viewportHelper: ExploreMapViewportHelper = ExploreMapViewportHelper(),
    val locationHelper: ExploreMapLocationHelper = ExploreMapLocationHelper(),
) : CommonsDaggerSupportFragment(),
    ExploreMapContract.View,
    ExploreMapContract.BottomSheet by detailsSheetHelper,
    ExploreMapContract.MarkerOverlay by markerOverlayHelper,
    ExploreMapContract.Viewport by viewportHelper,
    ExploreMapContract.Location by locationHelper,
    LocationUpdateListener by locationHelper,
    LocationPermissionCallback by locationHelper {
    var presenter: ExploreMapPresenter? = null

    @Inject
    lateinit var exploreMapController: ExploreMapController

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var liveDataConverter: LiveDataConverter

    @VisibleForTesting
    var binding: FragmentExploreMapBinding? = null

    @VisibleForTesting
    var mediaList: List<fr.free.nrw.commons.Media>? = null

    /**
     * The LatLng from the previous Fragment's map center or (0,0,0) coordinates
     * if that information is not available/applicable.
     */
    val previousLatLng: LatLng
        get() = viewportHelper.previousLatLng

    private var recentlyCameFromNearbyMap = false

    @VisibleForTesting
    var snackbar: Snackbar? = null

    @VisibleForTesting
    var isPermissionDenied: Boolean
        get() = viewportHelper.isPermissionDenied
        set(value) {
            viewportHelper.isPermissionDenied = value
        }

    var locationPermissionsHelper: LocationPermissionsHelper? = null
    private var isNetworkErrorOccurred = false

    internal val onLocationPermissionGrantedAction: () -> Unit = {
        isPermissionDenied = false
        applicationKvStore.putBoolean("doNotAskForLocationPermission", false)
        val target = locationManager.getLastLocation()
        locationHelper.lastKnownLocation = target
        if (target != null) {
            val targetP = GeoPoint(target.latitude, target.longitude)
            viewportHelper.mapCenter = targetP
            binding?.mapView?.controller?.setCenter(targetP)
            markerOverlayHelper.recenterMarkerToPosition(targetP)
            binding?.mapView?.controller?.animateTo(targetP)
        } else if (locationManager.isGPSProviderEnabled() || locationManager.isNetworkProviderEnabled()) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
            setProgressBarVisibility(true)
        } else {
            locationPermissionsHelper?.showLocationOffDialog(
                requireActivity(),
                R.string.ask_to_turn_location_on_text,
            )
        }
        presenter?.onMapReady(exploreMapController)
        locationHelper.registerUnregisterLocationListener(false)
    }

    internal val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onLocationPermissionGrantedAction()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showAlertDialog(
                        requireActivity(),
                        requireActivity().getString(R.string.location_permission_title),
                        requireActivity().getString(R.string.location_permission_rationale_explore),
                        requireActivity().getString(R.string.ok),
                        requireActivity().getString(R.string.cancel),
                        { askForLocationPermission() },
                        null,
                        null,
                    )
                } else {
                    if (isPermissionDenied) {
                        locationPermissionsHelper?.showAppSettingsDialog(
                            requireActivity(),
                            R.string.explore_map_needs_location,
                        )
                    }
                    Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                    isPermissionDenied = true
                }
            }
        }

    private val broadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (activity != null && isInternetConnectionEstablished(activity)) {
                    if (isNetworkErrorOccurred) {
                        presenter?.updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                        isNetworkErrorOccurred = false
                    }
                    if (snackbar != null) {
                        snackbar?.dismiss()
                        snackbar = null
                    }
                } else {
                    isNetworkErrorOccurred = true
                    setSearchThisAreaButtonVisibility(false)
                    if (snackbar == null && view != null) {
                        snackbar =
                            Snackbar.make(
                                requireView(),
                                R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE,
                            )
                        snackbar?.show()
                    }
                }
            }
        }

    /**
     * Inflates the map layout and initializes the view binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentExploreMapBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    /**
     * Configures helpers, map settings, presenter listeners, and parses navigation arguments.
     */
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding!!
        viewportHelper.loadNearbyMapData(arguments)
        setRecentlyCameFromNearbyMap(viewportHelper.isCameFromNearbyMap)
        if (presenter == null) {
            presenter = ExploreMapPresenter(bookmarkLocationDao)
        }
        locationPermissionsHelper = LocationPermissionsHelper(requireActivity(), locationManager, locationHelper)
        detailsSheetHelper.init(b.bottomSheetDetailsBinding)
        viewportHelper.attachViewportHelper(b, this)
        locationHelper.attachLocationHelper(this)
        markerOverlayHelper.attachMarkerOverlay(b, this)
        b.configureExploreMap(this)
        presenter?.attachView(this)
        presenter?.setActionListeners(applicationKvStore)
        // removed tha permission check here to prevent it from running on fragment creation
    }

    /**
     * Resumes map view lifecycle, registers network and location listeners, and checks permissions.
     */
    override fun onResume() {
        super.onResume()
        binding?.mapView?.onResume()
        presenter?.attachView(this)
        activity?.registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        locationHelper.registerUnregisterLocationListener(false)
        setSearchThisAreaButtonVisibility(false)
        locationHelper.checkAndPerformResumeMapReadyActions()
    }

    /**
     * Pauses map view lifecycle and unregisters network and location listeners.
     */
    override fun onPause() {
        super.onPause()
        binding?.mapView?.onPause()
        locationHelper.registerUnregisterLocationListener(true)
        // unregistering the broadcastReceiver, as it was causing an exception and a potential crash
        activity?.unregisterReceiver(broadcastReceiver)
        if (snackbar != null) {
            snackbar?.dismiss()
            snackbar = null
        }
        presenter?.detachView()
    }

    /**
     * Queries and populates nearby media places around the given coordinates.
     *
     * @param curlatLng Coordinates around which to search for places
     */
    override fun populatePlaces(curlatLng: LatLng?) {
        if (curlatLng == null) return
        val currentPresenter = presenter ?: return
        val lastFocus = getLastMapFocus()
        val observable: Observable<ExplorePlacesInfo?> =
            if (curlatLng == lastFocus) {
                // Means we are checking around current location
                currentPresenter.loadAttractionsFromLocation(curlatLng, lastFocus, true)
            } else {
                currentPresenter.loadAttractionsFromLocation(lastFocus, curlatLng, false)
            }
        compositeDisposable.add(
            observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { info ->
                        mediaList = info?.mediaList?.toMutableList()
                        if (mediaList.isNullOrEmpty()) {
                            showLongSnackbar(requireView(), getString(R.string.no_pictures_in_this_area))
                        }
                        if (info != null) currentPresenter.updateMapMarkers(info)
                        viewportHelper.lastMapFocus = GeoPoint(curlatLng.latitude, curlatLng.longitude)
                    },
                    Consumer { throwable ->
                        Timber.d(throwable)
                        // Not showing the user, throwable localizedErrorMessage
                        showLongToast(requireActivity(), getString(R.string.error_fetching_nearby_places))
                        setProgressBarVisibility(false)
                        currentPresenter.lockUnlockNearby(false)
                    },
                ),
        )
        viewportHelper.recenterToUserLocation = false
    }

    /**
     * Controls visibility of the map loading progress indicator.
     *
     * @param isVisible True to display progress bar, false to hide
     */
    override fun setProgressBarVisibility(isVisible: Boolean) {
        binding?.mapProgressBar?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Transitions from Explore map to Nearby map with the current camera zoom and center coordinates.
     */
    fun loadNearbyMapFromExplore() {
        val map = binding?.mapView ?: return
        (requireContext() as MainActivity).loadNearbyMapFromExplore(
            map.zoomLevelDouble,
            map.mapCenter.latitude,
            map.mapCenter.longitude,
        )
    }

    /**
     * Gets the value that indicates if the user navigated from "Show in Explore" in Nearby and
     * that the LatLng from Nearby has yet to be searched for map markers.
     */
    fun recentlyCameFromNearbyMap(): Boolean = recentlyCameFromNearbyMap

    /**
     * Sets the value that indicates if the user navigated from "Show in Explore" in Nearby and
     * that the LatLng from Nearby has yet to be searched for map markers.
     * @param newValue The value to set.
     */
    fun setRecentlyCameFromNearbyMap(newValue: Boolean) {
        recentlyCameFromNearbyMap = newValue
    }

    /**
     * Requests location permissions and updates if appropriate.
     */
    fun requestLocationIfNeeded() {
        locationHelper.requestLocationIfNeeded()
    }

    companion object {
        /**
         * Creates a new instance of ExploreMapFragment.
         *
         * @return New ExploreMapFragment instance
         */
        @Suppress("DEPRECATION")
        fun newInstance(): ExploreMapFragment =
            ExploreMapFragment().apply {
                retainInstance = true
            }
    }
}

private const val MIN_MAP_ZOOM = 1.0
private const val MAX_MAP_ZOOM = 22.0

/**
 * Performs map camera centering and presenter map-ready notifications once location
 * and viewport dependencies are initialized.
 */
fun ExploreMapFragment.performMapReadyActions() {
    if (applicationKvStore.getBoolean("doNotAskForLocationPermission", false) &&
        locationPermissionsHelper?.checkLocationPermission(requireActivity()) != true
    ) {
        isPermissionDenied = true
    }

    var lastLoc = locationHelper.getLastLocation()
    if (lastLoc == null) {
        lastLoc = defaultLatLng
    }
    locationHelper.lastKnownLocation = lastLoc

    val map = binding?.mapView
    if (viewportHelper.isCameFromNearbyMap) {
        val targetPoint = GeoPoint(viewportHelper.prevLatitude, viewportHelper.prevLongitude)
        viewportHelper.mapCenter = targetPoint
        map?.controller?.setCenter(targetPoint)
        map?.controller?.setZoom(viewportHelper.prevZoom.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM))
    } else {
        val targetPoint = GeoPoint(lastLoc.latitude, lastLoc.longitude)
        viewportHelper.mapCenter = targetPoint
        map?.controller?.setCenter(targetPoint)
    }
    presenter?.onMapReady(exploreMapController)
}
