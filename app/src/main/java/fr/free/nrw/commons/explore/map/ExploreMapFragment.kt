@file:Suppress("DEPRECATION")

package fr.free.nrw.commons.explore.map

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.ExploreMapRootFragment
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.MapUtils
import fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.showLongSnackbar
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.osmdroid.views.overlay.TilesOverlay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class ExploreMapFragment : CommonsDaggerSupportFragment(), ExploreMapContract.View,
    LocationUpdateListener, LocationPermissionCallback {
    private var bottomSheetDetailsBehavior: BottomSheetBehavior<*>? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isNetworkErrorOccurred = false
    private var snackbar: Snackbar? = null
    private var isDarkTheme = false
    private var isPermissionDenied = false
    private var lastKnownLocation: LatLng? = null // last location of user
    private var recenterToUserLocation = false // true is recenter is needed (ie. when current location is in visible map boundaries)
    private var clickedMarker: BaseMarker? = null
    private var mapCenter: GeoPoint? = null
    private var lastMapFocus: GeoPoint? = null
    private var intentFilter: IntentFilter = IntentFilter(MapUtils.NETWORK_INTENT_ACTION)
    private var baseMarkerOverlayMap: MutableMap<BaseMarker?, Overlay?>? = null
    private var locationPermissionsHelper: LocationPermissionsHelper? = null
    private var prevZoom = 0.0
    private var prevLatitude = 0.0
    private var prevLongitude = 0.0
    private var recentlyCameFromNearbyMap = false
    private var shouldPerformMapReadyActionsOnResume = false
    private var presenter: ExploreMapPresenter? = null
    private var binding: FragmentExploreMapBinding? = null
    var mediaList: MutableList<Media>? = null
        private set

    @Inject
    lateinit var liveDataConverter: LiveDataConverter

    @Inject
    lateinit var mediaClient: MediaClient

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var exploreMapController: ExploreMapController

    @Inject
    @Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var bookmarkLocationDao: BookmarkLocationsDao // May be needed in future if we want to integrate bookmarking explore places

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    private val activityResultLauncher = registerForActivityResult<String?, Boolean?>(
        RequestPermission()
    ) { isGranted: Boolean? ->
        if (isGranted == true) {
            locationPermissionGranted()
        } else {
            if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                showAlertDialog(
                    requireActivity(),
                    requireActivity().getString(R.string.location_permission_title),
                    requireActivity().getString(R.string.location_permission_rationale_explore),
                    requireActivity().getString(R.string.ok),
                    requireActivity().getString(R.string.cancel),
                    { askForLocationPermission() },
                    null,
                    null
                )
            } else {
                if (isPermissionDenied) {
                    locationPermissionsHelper!!.showAppSettingsDialog(
                        requireActivity(),
                        R.string.explore_map_needs_location
                    )
                }
                Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                isPermissionDenied = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadNearbyMapData()
        binding = FragmentExploreMapBinding.inflate(getLayoutInflater())
        return binding!!.getRoot()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSearchThisAreaButtonVisibility(false)
        binding!!.tvAttribution.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
             Html.fromHtml(getString(R.string.map_attribution), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(getString(R.string.map_attribution))
        }
        initNetworkBroadCastReceiver()
        locationPermissionsHelper = LocationPermissionsHelper(
            requireActivity(), locationManager,
            this
        )
        if (presenter == null) {
            presenter = ExploreMapPresenter(bookmarkLocationDao)
        }
        setHasOptionsMenu(true)

        isDarkTheme = systemThemeUtils.isDeviceInNightMode()
        isPermissionDenied = false
        presenter!!.attachView(this)

        initViews()
        presenter!!.setActionListeners(applicationKvStore)

        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding!!.mapView.setTileSource(TileSourceFactory.WIKIMEDIA)
        binding!!.mapView.setTilesScaledToDpi(true)

        Configuration.getInstance().additionalHttpRequestProperties.put(
            "Referer", "http://maps.wikimedia.org/"
        )

        val scaleBarOverlay = ScaleBarOverlay(binding!!.mapView)
        scaleBarOverlay.setScaleBarOffset(15, 25)
        val barPaint = Paint()
        barPaint.setARGB(200, 255, 250, 250)
        scaleBarOverlay.setBackgroundPaint(barPaint)
        scaleBarOverlay.enableScaleBar()
        binding!!.mapView.overlays.add(scaleBarOverlay)
        binding!!.mapView.zoomController
            .setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding!!.mapView.setMultiTouchControls(true)

        if (!isCameFromNearbyMap) {
            binding!!.mapView.controller.setZoom(ZOOM_LEVEL.toDouble())
        }


        binding!!.mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (clickedMarker != null) {
                    removeMarker(clickedMarker)
                    addMarkerToMap(clickedMarker!!)
                    binding!!.mapView.invalidate()
                } else {
                    Timber.e("CLICKED MARKER IS NULL")
                }
                if (bottomSheetDetailsBehavior!!.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    // Back should first hide the bottom sheet if it is expanded
                    bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
                } else if (isDetailsBottomSheetVisible()) {
                    hideBottomDetailsSheet()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }))

        binding!!.mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                if (getLastMapFocus() != null) {
                    val mylocation = Location("")
                    val dest_location = Location("")
                    dest_location.latitude = binding!!.mapView.mapCenter.latitude
                    dest_location.longitude = binding!!.mapView.mapCenter.longitude
                    mylocation.latitude = getLastMapFocus()!!.latitude
                    mylocation.longitude = getLastMapFocus()!!.longitude
                    val distance = mylocation.distanceTo(dest_location) //in meters
                    if (getLastMapFocus() != null) {
                        if (isNetworkConnectionEstablished() && (event.getX() > 0
                                    || event.getY() > 0)
                        ) {
                            setSearchThisAreaButtonVisibility(distance > 2000.0)
                        }
                    } else {
                        setSearchThisAreaButtonVisibility(false)
                    }
                }

                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean = false
        })
        // removed tha permission check here to prevent it from running on fragment creation
    }

    override fun onResume() {
        super.onResume()
        binding!!.mapView.onResume()
        presenter!!.attachView(this)
        locationManager.addLocationListener(this)
        if (broadcastReceiver != null) {
            requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        }
        setSearchThisAreaButtonVisibility(false)
        if (shouldPerformMapReadyActionsOnResume) {
            shouldPerformMapReadyActionsOnResume = false
            performMapReadyActions()
        }
    }

    override fun onPause() {
        super.onPause()
        // unregistering the broadcastReceiver, as it was causing an exception and a potential crash
        unregisterNetworkReceiver()
        locationManager.unregisterLocationManager()
        locationManager.removeLocationListener(this)
    }

    fun requestLocationIfNeeded() {
        if (isResumed) {
            performMapReadyActions()
        } else {
            shouldPerformMapReadyActionsOnResume = true
        }
        if (!isVisible) return  //  skips if not visible to user
        if (locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
            if (locationPermissionsHelper!!.isLocationAccessToAppsTurnedOn()) {
                locationManager.registerLocationManager()
                drawMyLocationMarker()
            } else {
                locationPermissionsHelper!!.showLocationOffDialog(requireActivity(), R.string.location_off_dialog_text)
            }
        } else {
            locationPermissionsHelper!!.requestForLocationAccess(
                R.string.location_permission_title,
                R.string.location_permission_rationale
            )
        }
    }

    private fun drawMyLocationMarker() {
        val location = locationManager.getLastLocation()
        if (location != null) {
            val geoPoint = GeoPoint(location.latitude, location.longitude)
            val startMarker = Marker(binding!!.mapView).apply {
                setPosition(geoPoint)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.current_location_marker)
                title = "Your Location"
                textLabelFontSize = 24
            }
            binding!!.mapView.overlays.add(startMarker)
            binding!!.mapView.invalidate()
        }
    }

    /**
     * Unregisters the networkReceiver
     */
    private fun unregisterNetworkReceiver() =
        activity?.unregisterReceiver(broadcastReceiver)

    private fun startMapWithoutPermission() {
        lastKnownLocation = defaultLatLng
        moveCameraToPosition(
            GeoPoint(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
        )
        presenter!!.onMapReady(exploreMapController)
    }

    private fun registerNetworkReceiver() =
        activity?.registerReceiver(broadcastReceiver, intentFilter)

    private fun performMapReadyActions() {
        if (isDarkTheme) {
            binding!!.mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        if (applicationKvStore.getBoolean("doNotAskForLocationPermission", false) &&
            !locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
            isPermissionDenied = true
        }

        lastKnownLocation = getLastLocation()

        if (lastKnownLocation == null) {
            lastKnownLocation = defaultLatLng
        }

        // if we came from 'Show in Explore' in Nearby, load Nearby map center and zoom
        if (isCameFromNearbyMap) {
            moveCameraToPosition(
                GeoPoint(prevLatitude, prevLongitude),
                prevZoom.coerceIn(1.0, 22.0),
                1L
            )
        } else {
            moveCameraToPosition(
                GeoPoint(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            )
        }
        presenter!!.onMapReady(exploreMapController)
    }

    /**
     * Fetch Nearby map camera data from fragment arguments if any.
     */
    fun loadNearbyMapData() {
        // get fragment arguments
        if (arguments != null) {
            with (requireArguments()) {
                if (containsKey("prev_zoom")) prevZoom = getDouble("prev_zoom")
                if (containsKey("prev_latitude")) prevLatitude = getDouble("prev_latitude")
                if (containsKey("prev_longitude")) prevLongitude = getDouble("prev_longitude")
            }
        }
        setRecentlyCameFromNearbyMap(isCameFromNearbyMap)
    }

    /**
     * @return The LatLng from the previous Fragment's map center or (0,0,0) coordinates
     * if that information is not available/applicable.
     */
    val previousLatLng: LatLng
        get() = LatLng(prevLatitude, prevLongitude, prevZoom.toFloat())

    /**
     * Checks if fragment arguments contain data from Nearby map, indicating that the user navigated
     * from Nearby using 'Show in Explore'.
     *
     * @return true if user navigated from Nearby map
     */
    val isCameFromNearbyMap: Boolean
        get() = prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0

    /**
     * Gets the value that indicates if the user navigated from "Show in Explore" in Nearby and
     * that the LatLng from Nearby has yet to be searched for map markers.
     */
    fun recentlyCameFromNearbyMap(): Boolean =
        recentlyCameFromNearbyMap

    /**
     * Sets the value that indicates if the user navigated from "Show in Explore" in Nearby and
     * that the LatLng from Nearby has yet to be searched for map markers.
     * @param newValue The value to set.
     */
    fun setRecentlyCameFromNearbyMap(newValue: Boolean) {
        recentlyCameFromNearbyMap = newValue
    }

    fun loadNearbyMapFromExplore() {
        (requireContext() as MainActivity).loadNearbyMapFromExplore(
            binding!!.mapView.zoomLevelDouble,
            binding!!.mapView.mapCenter.latitude,
            binding!!.mapView.mapCenter.longitude
        )
    }

    private fun initViews() {
        initBottomSheets()
        setBottomSheetCallbacks()
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheet, sets initial states and visibility
     * b) Gets the touch event on the map to perform following actions:
     * if bottom sheet details are expanded or collapsed hide the bottom sheet details.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initBottomSheets() {
        bottomSheetDetailsBehavior = BottomSheetBehavior.from<LinearLayout?>(
            binding!!.bottomSheetDetailsBinding.getRoot()
        )
        bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
        binding!!.bottomSheetDetailsBinding.getRoot().visibility = View.VISIBLE
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private fun setBottomSheetCallbacks() {
        binding!!.bottomSheetDetailsBinding.getRoot()
            .setOnClickListener { v: View? ->
                if (bottomSheetDetailsBehavior!!.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
                } else if (bottomSheetDetailsBehavior!!.getState()
                    == BottomSheetBehavior.STATE_EXPANDED
                ) {
                    bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
                }
            }
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) =
        handleLocationUpdate(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)

    override fun onLocationChangedSlightly(latLng: LatLng) =
        handleLocationUpdate(latLng, LocationChangeType.LOCATION_SLIGHTLY_CHANGED)

    private fun handleLocationUpdate(
        latLng: LatLng?,
        locationChangeType: LocationChangeType
    ) {
        lastKnownLocation = latLng
        exploreMapController.currentLocation = lastKnownLocation
        presenter!!.updateMap(locationChangeType)
    }

    override fun onLocationChangedMedium(latLng: LatLng) = Unit

    override fun isNetworkConnectionEstablished(): Boolean =
        isInternetConnectionEstablished(requireActivity())

    override fun populatePlaces(curlatLng: LatLng?) {
        val nearbyPlacesInfoObservable: Observable<ExplorePlacesInfo?>
        if (curlatLng == null) {
            return
        }
        if (curlatLng.equals(
                getLastMapFocus()
            )
        ) { // Means we are checking around current location
            nearbyPlacesInfoObservable = presenter!!.loadAttractionsFromLocation(
                curlatLng,
                getLastMapFocus(), true
            )
        } else {
            nearbyPlacesInfoObservable = presenter!!.loadAttractionsFromLocation(
                getLastMapFocus(),
                curlatLng, false
            )
        }
        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    Consumer { explorePlacesInfo: ExplorePlacesInfo? ->
                        mediaList = explorePlacesInfo!!.mediaList.toMutableList()
                        if (mediaList!!.isEmpty()) {
                            showResponseMessage(getString(R.string.no_pictures_in_this_area))
                        }
                        updateMapMarkers(explorePlacesInfo)
                        lastMapFocus = GeoPoint(
                            curlatLng.latitude,
                            curlatLng.longitude
                        )
                    },
                    Consumer { throwable: Throwable? ->
                        Timber.d(throwable)
                        // Not showing the user, throwable localizedErrorMessage
                        showErrorMessage(getString(R.string.error_fetching_nearby_places))

                        setProgressBarVisibility(false)
                        presenter!!.lockUnlockNearby(false)
                    })
        )
        if (recenterToUserLocation) {
            recenterToUserLocation = false
        }
    }

    /**
     * Updates map markers according to latest situation
     *
     * @param explorePlacesInfo holds several information as current location, marker list etc.
     */
    private fun updateMapMarkers(explorePlacesInfo: ExplorePlacesInfo) =
        presenter!!.updateMapMarkers(explorePlacesInfo)

    private fun showErrorMessage(message: String) =
        showLongToast(requireActivity(), message)

    private fun showResponseMessage(message: String) =
        showLongSnackbar(requireView(), message)

    override fun askForLocationPermission() {
        Timber.d("Asking for location permission")
        activityResultLauncher.launch(permission.ACCESS_FINE_LOCATION)
    }

    private fun locationPermissionGranted() {
        isPermissionDenied = false
        applicationKvStore.putBoolean("doNotAskForLocationPermission", false)
        lastKnownLocation = locationManager.getLastLocation()
        val target = lastKnownLocation
        if (lastKnownLocation != null) {
            val targetP = GeoPoint(target!!.latitude, target.longitude)
            mapCenter = targetP
            binding!!.mapView.controller.setCenter(targetP)
            recenterMarkerToPosition(targetP)
            moveCameraToPosition(targetP)
        } else if (locationManager.isGPSProviderEnabled()
            || locationManager.isNetworkProviderEnabled()
        ) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
            setProgressBarVisibility(true)
        } else {
            locationPermissionsHelper!!.showLocationOffDialog(
                requireActivity(),
                R.string.ask_to_turn_location_on_text
            )
        }
        presenter!!.onMapReady(exploreMapController)
        registerUnregisterLocationListener(false)
    }

    fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
        MapUtils.registerUnregisterLocationListener(removeLocationListener, locationManager, this)
    }

    override fun recenterMap(curLatLng: LatLng?) {
        // if user has denied permission twice, then show dialog
        if (isPermissionDenied) {
            if (locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
                // this will run when user has given permission by opening app's settings
                isPermissionDenied = false
                recenterMap(curLatLng)
            } else {
                askForLocationPermission()
            }
        } else {
            if (!locationPermissionsHelper!!.checkLocationPermission(requireActivity())) {
                askForLocationPermission()
            } else {
                locationPermissionGranted()
            }
        }
        if (curLatLng == null) {
            recenterToUserLocation = true
            return
        }
        recenterMarkerToPosition(
            GeoPoint(curLatLng.latitude, curLatLng.longitude)
        )
        binding!!.mapView.controller.animateTo(
            GeoPoint(curLatLng.latitude, curLatLng.longitude)
        )
        if (lastMapFocus != null) {
            val mylocation = Location("")
            val dest_location = Location("")
            dest_location.latitude = binding!!.mapView.mapCenter.latitude
            dest_location.longitude = binding!!.mapView.mapCenter.longitude
            mylocation.latitude = lastMapFocus!!.latitude
            mylocation.longitude = lastMapFocus!!.longitude
            val distance = mylocation.distanceTo(dest_location) //in meters
            if (lastMapFocus != null) {
                if (isNetworkConnectionEstablished()) {
                    setSearchThisAreaButtonVisibility(distance > 2000.0)
                }
            } else {
                setSearchThisAreaButtonVisibility(false)
            }
        }
    }

    override fun hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private fun passInfoToSheet(place: Place) {
        binding!!.bottomSheetDetailsBinding.directionsButton.setOnClickListener {
            handleGeoCoordinates(requireActivity(), place.getLocation(), binding!!.mapView.zoomLevelDouble)
        }

        binding!!.bottomSheetDetailsBinding.commonsButton.visibility = if (place.hasCommonsLink()) View.VISIBLE else View.GONE
        binding!!.bottomSheetDetailsBinding.commonsButton.setOnClickListener {
            handleWebUrl(requireContext(), place.siteLinks.commonsLink)
        }

        var index = 0
        for (media in mediaList!!) {
            if (media.filename == place.name) {
                val finalIndex = index
                binding!!.bottomSheetDetailsBinding.mediaDetailsButton.setOnClickListener {
                    (parentFragment as ExploreMapRootFragment).onMediaClicked(finalIndex)
                }
            }
            index++
        }
        binding!!.bottomSheetDetailsBinding.title.text = place.name.substring(5, place.name.lastIndexOf("."))
        binding!!.bottomSheetDetailsBinding.category.text = place.distance
        // Remove label since it is double information
        var descriptionText = place.longDescription
            .replace(place.getName() + " (", "")
        descriptionText = (if (descriptionText == place.longDescription)
            descriptionText
        else
            descriptionText.replaceFirst(".$".toRegex(), ""))
        // Set the short description after we remove place name from long description
        binding!!.bottomSheetDetailsBinding.description.text = descriptionText
    }

    override fun addSearchThisAreaButtonAction() {
        binding!!.searchThisAreaButton.setOnClickListener(presenter!!.onSearchThisAreaClicked())
    }

    override fun setSearchThisAreaButtonVisibility(isVisible: Boolean) {
        binding!!.searchThisAreaButton.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun setProgressBarVisibility(isVisible: Boolean) {
        binding!!.mapProgressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun isDetailsBottomSheetVisible(): Boolean =
        binding!!.bottomSheetDetailsBinding.getRoot().isVisible

    override fun isSearchThisAreaButtonVisible(): Boolean =
        binding!!.bottomSheetDetailsBinding.getRoot().isVisible

    override fun getLastLocation(): LatLng? {
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastLocation()
        }
        return lastKnownLocation
    }

    override fun disableFABRecenter() {
        binding!!.fabRecenter.setEnabled(false)
    }

    override fun enableFABRecenter() {
        binding!!.fabRecenter.setEnabled(true)
    }

    /**
     * Adds a markers to the map based on the list of NearbyBaseMarker.
     *
     * @param nearbyBaseMarkers The NearbyBaseMarker object representing the markers to be added.
     */
    override fun addMarkersToMap(nearbyBaseMarkers: List<BaseMarker?>?) {
        clearAllMarkers()
        nearbyBaseMarkers?.forEach {
            addMarkerToMap(it!!)
        }
        binding!!.mapView.invalidate()
    }

    /**
     * Adds a marker to the map based on the specified NearbyBaseMarker.
     *
     * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be added.
     */
    private fun addMarkerToMap(nearbyBaseMarker: BaseMarker) {
        if (isAttachedToActivity) {
            val items = mutableListOf<OverlayItem?>()
            val d: Drawable = nearbyBaseMarker.icon!!.toDrawable(resources)
            val point = GeoPoint(
                nearbyBaseMarker.place.location.latitude,
                nearbyBaseMarker.place.location.longitude
            )

            val markerMedia = getMediaFromImageURL(nearbyBaseMarker.place.pic)
            var authorUser: String? = null
            if (markerMedia != null) {
                // HTML text is sometimes part of the author string and needs to be removed
                authorUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(markerMedia.getAuthorOrUser(), Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(markerMedia.getAuthorOrUser())
                }.toString()
            }

            var title = nearbyBaseMarker.place.name
            // Remove "File:" if present at start
            if (title.startsWith("File:")) {
                title = title.substring(5)
            }
            // Remove extensions like .jpg, .jpeg, .png, .svg (case insensitive)
            title = title.replace("(?i)\\.(jpg|jpeg|png|svg)$".toRegex(), "")
            title = title.replace("_", " ")
            //Truncate if too long because it doesn't fit the screen
            if (title.length > 43) {
                title = title.substring(0, 40) + "…"
            }

            val item = OverlayItem(title, authorUser, point)
            item.setMarker(d)
            items.add(item)
            val overlay = ItemizedOverlayWithFocus<OverlayItem?>(
                items,
                object : OnItemGestureListener<OverlayItem?> {
                    override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                        val place = nearbyBaseMarker.place
                        if (clickedMarker != null) {
                            removeMarker(clickedMarker)
                            addMarkerToMap(clickedMarker!!)
                            bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
                            bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
                        }
                        clickedMarker = nearbyBaseMarker
                        passInfoToSheet(place)

                        //Move the overlay to the top so it can be fully seen.
                        moveOverlayToTop(getOverlay(item))
                        return true
                    }

                    override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean = false
                }, requireContext()
            )

            if (baseMarkerOverlayMap == null) {
                baseMarkerOverlayMap = HashMap<BaseMarker?, Overlay?>()
            }
            baseMarkerOverlayMap!!.put(nearbyBaseMarker, overlay)

            overlay.setFocusItemsOnTap(true)
            binding!!.mapView.overlays.add(overlay) // Add the overlay to the map
        }
    }

    /**
     * Moves the specified Overlay above all other Overlays. This prevents other Overlays from
     * obstructing it. Upon failure, this method returns early.
     * @param overlay The Overlay to move.
     */
    private fun moveOverlayToTop(overlay: Overlay?) {
        if (overlay == null || binding == null || binding!!.mapView.overlays == null) {
            return
        }

        val successfulRemoval = binding!!.mapView.overlays.remove(overlay)
        if (!successfulRemoval) {
            return
        }

        binding!!.mapView.overlays.add(overlay)
    }

    /**
     * Performs a linear search for the first Overlay which contains the specified OverlayItem.
     *
     * @param item The OverlayItem contained within the first target Overlay.
     * @return The first Overlay which contains the specified OverlayItem or null if the Overlay
     * could not be found.
     */
    private fun getOverlay(item: OverlayItem?): Overlay? {
        if (item == null || binding == null || binding!!.mapView.overlays == null) {
            return null
        }

        for (i in binding!!.mapView.overlays.indices) {
            if (binding!!.mapView.overlays[i] is ItemizedOverlayWithFocus<*>) {
                val overlay = binding!!.mapView.overlays[i] as ItemizedOverlayWithFocus<*>

                for (j in 0..<overlay.size()) {
                    if (overlay.getItem(j) === item) {
                        return overlay
                    }
                }
            }
        }

        return null
    }

    /**
     * Retrieves the specific Media object from the mediaList field.
     * @param url The specific Media's image URL.
     * @return The Media object that matches the URL or null if it could not be found.
     */
    private fun getMediaFromImageURL(url: String?): Media? {
        if (mediaList == null || url == null) {
            return null
        }

        for (i in mediaList!!.indices) {
            if (mediaList!![i].imageUrl != null && mediaList!![i].imageUrl == url) {
                return mediaList!![i]
            }
        }

        return null
    }

    /**
     * Removes a marker from the map based on the specified NearbyBaseMarker.
     *
     * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be removed.
     */
    private fun removeMarker(nearbyBaseMarker: BaseMarker?) {
        if (nearbyBaseMarker == null ||
            nearbyBaseMarker.place.getName() == null ||
            baseMarkerOverlayMap == null ||
            !baseMarkerOverlayMap!!.containsKey(nearbyBaseMarker)) {
            return
        }

        val target = baseMarkerOverlayMap!![nearbyBaseMarker]
        val overlays = binding!!.mapView.overlays

        for (i in overlays.indices) {
            val overlay = overlays[i]

            if (overlay == target) {
                binding!!.mapView.overlays.removeAt(i)
                binding!!.mapView.invalidate()
                baseMarkerOverlayMap!!.remove(nearbyBaseMarker)
                break
            }
        }
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    override fun clearAllMarkers() {
        if (isAttachedToActivity) {
            binding!!.mapView.overlayManager.clear()

            if (mapCenter != null) {
                val diskOverlay = ScaleDiskOverlay(
                    requireContext(),
                    mapCenter,
                    2000,
                    GeoConstants.UnitOfMeasure.foot
                ).apply {
                    setCirclePaint2(Paint().apply {
                        setColor(Color.rgb(128, 128, 128))
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    })
                    setCirclePaint1(Paint().apply {
                        setColor(Color.argb(40, 128, 128, 128))
                        style = Paint.Style.FILL_AND_STROKE
                    })
                    setDisplaySizeMin(900)
                    setDisplaySizeMax(1700)
                }
                binding!!.mapView.overlays.add(diskOverlay)

                binding!!.mapView.overlays.add(Marker(binding!!.mapView).apply {
                    setPosition(mapCenter)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.current_location_marker
                    )
                    title = "Your Location"
                    textLabelFontSize = 24
                })
            }

            binding!!.mapView.overlays.add(ScaleBarOverlay(binding!!.mapView).apply {
                setScaleBarOffset(15, 25)
                setBackgroundPaint(Paint().apply<Paint> {
                    setARGB(200, 255, 250, 250)
                })
                enableScaleBar()
            })

            binding!!.mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (clickedMarker != null) {
                        removeMarker(clickedMarker)
                        addMarkerToMap(clickedMarker!!)
                        binding!!.mapView.invalidate()
                    } else {
                        Timber.e("CLICKED MARKER IS NULL")
                    }
                    if (bottomSheetDetailsBehavior!!.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        // Back should first hide the bottom sheet if it is expanded
                        bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
                    } else if (isDetailsBottomSheetVisible()) {
                        hideBottomDetailsSheet()
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }))
            binding!!.mapView.setMultiTouchControls(true)
        }
    }

    /**
     * Recenters the map view to the specified GeoPoint and updates the marker to indicate the new
     * position.
     *
     * @param geoPoint The GeoPoint representing the new center position for the map.
     */
    private fun recenterMarkerToPosition(geoPoint: GeoPoint?) {
        if (geoPoint != null) {
            binding!!.mapView.controller.setCenter(geoPoint)
            val overlays = binding!!.mapView.overlays
            // collects the indices of items to remove
            val indicesToRemove = mutableListOf<Int>()
            for (i in overlays.indices) {
                if (overlays[i] is Marker || overlays[i] is ScaleDiskOverlay) {
                    indicesToRemove.add(i)
                }
            }
            // removes the items in reverse order to avoid index shifting
            indicesToRemove.sortedDescending().forEach { index ->
                binding!!.mapView.overlays.removeAt(index)
            }
            val diskOverlay = ScaleDiskOverlay(
                requireContext(),
                geoPoint, 2000, GeoConstants.UnitOfMeasure.foot
            ).apply {
                setCirclePaint2(Paint().apply {
                    setColor(Color.rgb(128, 128, 128))
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = 2f
                })
                setCirclePaint1(Paint().apply {
                    setColor(Color.argb(40, 128, 128, 128))
                    this.style = Paint.Style.FILL_AND_STROKE
                })
                setDisplaySizeMin(900)
                setDisplaySizeMax(1700)
            }
            binding!!.mapView.overlays.add(diskOverlay)
            val startMarker = Marker(
                binding!!.mapView
            ).apply {
                setPosition(geoPoint)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.current_location_marker)
                title = "Your Location"
                textLabelFontSize = 24
            }
            binding!!.mapView.overlays.add(startMarker)
        }
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     */
    private fun moveCameraToPosition(geoPoint: GeoPoint?) {
        binding!!.mapView.controller.animateTo(geoPoint)
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint at specified zoom level and speed
     * using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     * @param zoom     Zoom level of the map camera
     * @param speed    Speed of animation
     */
    private fun moveCameraToPosition(geoPoint: GeoPoint?, zoom: Double, speed: Long) {
        binding!!.mapView.controller.animateTo(geoPoint, zoom, speed)
    }

    override fun getLastMapFocus(): LatLng? = if (lastMapFocus == null) {
        getMapCenter()
    } else {
        LatLng(lastMapFocus!!.latitude, lastMapFocus!!.longitude, 100f)
    }

    override fun getMapCenter(): LatLng? = if (mapCenter != null) {
        LatLng(mapCenter!!.latitude, mapCenter!!.longitude, 100f)
    } else {
        if (applicationKvStore.getString("LastLocation") != null) {
            val locationLatLng: Array<String?> =
                applicationKvStore.getString("LastLocation")!!
                    .split(",".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray<String?>()
            lastKnownLocation = LatLng(
                locationLatLng[0]!!.toDouble(),
                locationLatLng[1]!!.toDouble(), 1f
            )
            lastKnownLocation
        } else {
            LatLng(51.506255446947776, -0.07483536015053005, 1f)
        }
    }

    override fun getMapFocus(): LatLng? = LatLng(
        binding!!.mapView.mapCenter.latitude,
        binding!!.mapView.mapCenter.longitude, 100f
    )

    override fun setFABRecenterAction(onClickListener: View.OnClickListener?) {
        binding!!.fabRecenter.setOnClickListener(onClickListener)
    }

    override fun backButtonClicked(): Boolean {
        if (bottomSheetDetailsBehavior!!.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetDetailsBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
            return true
        } else {
            return false
        }
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private fun initNetworkBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (activity != null) {
                    if (isInternetConnectionEstablished(requireActivity())) {
                        if (isNetworkErrorOccurred) {
                            presenter!!.updateMap(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                            isNetworkErrorOccurred = false
                        }

                        if (snackbar != null) {
                            snackbar!!.dismiss()
                            snackbar = null
                        }
                    } else {
                        if (snackbar == null) {
                            snackbar = Snackbar.make(
                                requireView(), R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE
                            )
                            setSearchThisAreaButtonVisibility(false)
                            setProgressBarVisibility(false)
                        }

                        isNetworkErrorOccurred = true
                        snackbar!!.show()
                    }
                }
            }
        }
    }

    val isAttachedToActivity: Boolean
        get() = isVisible && activity != null

    override fun onLocationPermissionDenied(toastMessage: String) = Unit

    override fun onLocationPermissionGranted() {
        if (locationPermissionsHelper!!.isLocationAccessToAppsTurnedOn()) {
            locationManager.registerLocationManager()
            drawMyLocationMarker()
        } else {
            locationPermissionsHelper!!.showLocationOffDialog(requireActivity(), R.string.location_off_dialog_text)
        }
        onLocationChanged(LocationChangeType.PERMISSION_JUST_GRANTED, null)
    }

    fun onLocationChanged(locationChangeType: LocationChangeType, location: Location?) {
        if (locationChangeType == LocationChangeType.PERMISSION_JUST_GRANTED) {
            val curLatLng = locationManager.getLastLocation() ?: getMapCenter()
            populatePlaces(curLatLng)
        } else {
            presenter!!.updateMap(locationChangeType)
        }
    }

    companion object {
        fun newInstance(): ExploreMapFragment {
            val fragment = ExploreMapFragment()
            fragment.setRetainInstance(true)
            return fragment
        }
    }
}
