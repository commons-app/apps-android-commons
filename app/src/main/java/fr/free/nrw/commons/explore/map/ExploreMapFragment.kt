package fr.free.nrw.commons.explore.map

import android.Manifest
import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
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
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.MapUtils
import fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL
import fr.free.nrw.commons.utils.NetworkUtils
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Named
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
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.osmdroid.views.overlay.TilesOverlay
import timber.log.Timber

class ExploreMapFragment : CommonsDaggerSupportFragment(),
    ExploreMapContract.View, LocationUpdateListener, LocationPermissionCallback {

    private lateinit var bottomSheetDetailsBehavior: BottomSheetBehavior<*>
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isNetworkErrorOccurred = false
    private var snackbar: Snackbar? = null
    private var isDarkTheme = false
    private var isPermissionDenied = false
    private var lastKnownLocation: fr.free.nrw.commons.location.LatLng? = null // last location of user
    private var lastFocusLocation: fr.free.nrw.commons.location.LatLng? = null // last focused location of the map
    var mediaList: List<Media>? = null
    private var recenterToUserLocation = false // true if recentering is needed
    private var clickedMarker: BaseMarker? = null
    private var mapCenter: GeoPoint? = null
    private var lastMapFocus: GeoPoint? = null
    private val intentFilter = IntentFilter(MapUtils.NETWORK_INTENT_ACTION)

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
    lateinit var bookmarkLocationDao: BookmarkLocationsDao // Future use for bookmarking explore places

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    private lateinit var locationPermissionsHelper: LocationPermissionsHelper

    // Nearby map state (if we came from Nearby)
    private var prevZoom = 0.0
    private var prevLatitude = 0.0
    private var prevLongitude = 0.0

    private var presenter: ExploreMapPresenter? = null

    private lateinit var binding: FragmentExploreMapBinding

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationPermissionGranted()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    activity?.let {
                        DialogUtil.showAlertDialog(
                            it,
                            getString(R.string.location_permission_title),
                            getString(R.string.location_permission_rationale_explore),
                            getString(android.R.string.ok),
                            getString(android.R.string.cancel),
                            {
                                askForLocationPermission()
                            },
                            null,
                            null
                        )
                    }
                } else {
                    if (isPermissionDenied) {
                        locationPermissionsHelper.showAppSettingsDialog(
                            requireActivity(),
                            R.string.explore_map_needs_location
                        )
                    }
                    Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                    isPermissionDenied = true
                }
            }
        }

    companion object {
        @JvmStatic
        fun newInstance(): ExploreMapFragment {
            return ExploreMapFragment().apply { retainInstance = true }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadNearbyMapData()
        binding = FragmentExploreMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSearchThisAreaButtonVisibility(false)

        binding.tvAttribution.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(getString(R.string.map_attribution), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(getString(R.string.map_attribution))
        }

        initNetworkBroadCastReceiver()
        locationPermissionsHelper = LocationPermissionsHelper(
            requireActivity(),
            locationManager,
            this
        )

        if (presenter == null) {
            presenter = ExploreMapPresenter(bookmarkLocationDao)
        }
        setHasOptionsMenu(true)

        isDarkTheme = systemThemeUtils.isDeviceInNightMode()
        isPermissionDenied = false
        presenter?.attachView(this)

        initViews()
        presenter?.setActionListeners(applicationKvStore)

        org.osmdroid.config.Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.mapView.apply {
            setTileSource(TileSourceFactory.WIKIMEDIA)
            setTilesScaledToDpi(true)
            org.osmdroid.config.Configuration.getInstance()
                .additionalHttpRequestProperties["Referer"] = "http://maps.wikimedia.org/"

            val scaleBarOverlay = ScaleBarOverlay(this).apply {
                setScaleBarOffset(15, 25)
                setBackgroundPaint(
                    Paint().apply {
                        setARGB(200, 255, 250, 250)
                    }
                )
                enableScaleBar()
            }
            overlays.add(scaleBarOverlay)

            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)

            if (!isCameFromNearbyMap()) {
                controller.setZoom(ZOOM_LEVEL.toDouble())
            }
        }

        performMapReadyActions()

        binding.mapView.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    clickedMarker?.let {
                        removeMarker(it)
                        addMarkerToMap(it)
                        binding.mapView.invalidate()
                    } ?: Timber.e("CLICKED MARKER IS NULL")

                    if (bottomSheetDetailsBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    } else if (isDetailsBottomSheetVisible()) {
                        hideBottomDetailsSheet()
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            })
        )


        binding.mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                lastMapFocus?.let {
                    val myLocation = Location("").apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }

                    val destLocation = Location("").apply {
                        latitude = binding.mapView.mapCenter.latitude
                        longitude = binding.mapView.mapCenter.longitude
                    }

                    val distance = myLocation.distanceTo(destLocation)

                    if (
                        isNetworkConnectionEstablished()
                        &&
                        (event?.x!! > 0 || event.y > 0)
                    ) {
                        setSearchThisAreaButtonVisibility(distance > 2000.0)
                    }
                } ?: setSearchThisAreaButtonVisibility(false)

                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean = false
        })

        if (!locationPermissionsHelper.checkLocationPermission(requireActivity())) {
            askForLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        presenter?.attachView(this)
        registerNetworkReceiver()

        if (isResumed) {
            if (activity?.let { locationPermissionsHelper.checkLocationPermission(it) } == true) {
                performMapReadyActions()
            } else {
                startMapWithoutPermission()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregistering the broadcastReceiver to prevent crashes
        unregisterNetworkReceiver()
    }

    /**
     * Unregisters the networkReceiver
     */
    private fun unregisterNetworkReceiver() {
        activity?.unregisterReceiver(broadcastReceiver)
    }

    private fun startMapWithoutPermission() {
        lastKnownLocation = MapUtils.defaultLatLng
        moveCameraToPosition(GeoPoint(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude))
        presenter?.onMapReady(exploreMapController)
    }

    private fun registerNetworkReceiver() {
        activity?.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun performMapReadyActions() {
        if (isDarkTheme) {
            binding.mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
        if (applicationKvStore.getBoolean("doNotAskForLocationPermission", false) &&
            !locationPermissionsHelper.checkLocationPermission(requireActivity())
        ) {
            isPermissionDenied = true
        }

        lastKnownLocation = MapUtils.defaultLatLng

        // If user came from 'Show in Explore' in Nearby, load saved map center and zoom
        if (isCameFromNearbyMap()) {
            moveCameraToPosition(GeoPoint(prevLatitude, prevLongitude), prevZoom, 1L)
        } else {
            moveCameraToPosition(
                GeoPoint(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
            )
        }

        presenter?.onMapReady(exploreMapController)
    }

    /**
     * Fetch Nearby map camera data from fragment arguments if available.
     */
    fun loadNearbyMapData() {
        arguments?.let {
            prevZoom = it.getDouble("prev_zoom")
            prevLatitude = it.getDouble("prev_latitude")
            prevLongitude = it.getDouble("prev_longitude")
        }
    }

    /**
     * Checks if fragment arguments contain data from the Nearby map,
     * indicating that the user navigated from Nearby using 'Show in Explore'.
     *
     * @return true if user navigated from Nearby map
     */
    fun isCameFromNearbyMap(): Boolean {
        return prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0
    }

    fun loadNearbyMapFromExplore() {
        (requireContext() as MainActivity).loadNearbyMapFromExplore(
            binding.mapView.zoomLevelDouble,
            binding.mapView.mapCenter.latitude,
            binding.mapView.mapCenter.longitude
        )
    }

    private fun initViews() {
        Timber.d("init views called")
        initBottomSheets()
        setBottomSheetCallbacks()
    }

    /**
     * a) Creates bottom sheet behaviors from bottom sheet, sets initial states and visibility.
     * b) Gets the touch event on the map to perform following actions:
     *      - If bottom sheet details are expanded or collapsed, hide the bottom sheet details.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initBottomSheets() {
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(binding.bottomSheetDetailsBinding.root)
        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        binding.bottomSheetDetailsBinding.root.visibility = View.VISIBLE
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private fun setBottomSheetCallbacks() {
        binding.bottomSheetDetailsBinding.root.setOnClickListener {
            bottomSheetDetailsBehavior.state = when (bottomSheetDetailsBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                else -> bottomSheetDetailsBehavior.state
            }
        }
    }

    override fun onLocationChangedSignificantly(latLng: LatLng?) {
        Timber.d("Location significantly changed")
        latLng?.let { handleLocationUpdate(it, LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED) }
    }

    override fun onLocationChangedSlightly(latLng: LatLng?) {
        Timber.d("Location slightly changed")
        latLng?.let { handleLocationUpdate(it, LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED) }
    }

    private fun handleLocationUpdate(
        latLng: fr.free.nrw.commons.location.LatLng,
        locationChangeType: LocationServiceManager.LocationChangeType
    ) {
        lastKnownLocation = latLng
        exploreMapController.currentLocation = lastKnownLocation
        presenter?.updateMap(locationChangeType)
    }

    override fun onLocationChangedMedium(latLng: LatLng?) {
        // No implementation required
    }

    override fun isNetworkConnectionEstablished(): Boolean {
        return NetworkUtils.isInternetConnectionEstablished(activity)
    }

    override fun populatePlaces(curLatLng: LatLng?) {
        if (curLatLng == null) return

        val nearbyPlacesInfoObservable: Observable<MapController.ExplorePlacesInfo> =
            if (curLatLng == getLastMapFocus()) {
            // Checking around current location
            presenter!!.loadAttractionsFromLocation(curLatLng, getLastMapFocus(), true)
        } else {
            presenter!!.loadAttractionsFromLocation(getLastMapFocus(), curLatLng, false)
        }

        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ explorePlacesInfo ->
                    mediaList = explorePlacesInfo.mediaList
                    if (mediaList.isNullOrEmpty()) {
                        showResponseMessage(getString(R.string.no_pictures_in_this_area))
                    }
                    updateMapMarkers(explorePlacesInfo)
                    lastMapFocus = GeoPoint(curLatLng.latitude, curLatLng.longitude)
                }, { throwable ->
                    Timber.d(throwable)
                    showErrorMessage(getString(R.string.error_fetching_nearby_places))
                    setProgressBarVisibility(false)
                    presenter?.lockUnlockNearby(false)
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
    private fun updateMapMarkers(explorePlacesInfo: MapController.ExplorePlacesInfo) {
        presenter?.updateMapMarkers(explorePlacesInfo)
    }

    private fun showErrorMessage(message: String) {
        ViewUtil.showLongToast(requireActivity(), message)
    }

    private fun showResponseMessage(message: String) {
        ViewUtil.showLongSnackbar(requireView(), message)
    }

    override fun askForLocationPermission() {
        Timber.d("Asking for location permission")
        activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun locationPermissionGranted() {
        isPermissionDenied = false
        applicationKvStore.putBoolean("doNotAskForLocationPermission", false)
        lastKnownLocation = locationManager.getLastLocation()
        val target = lastKnownLocation

        if (lastKnownLocation != null) {
            val targetP = GeoPoint(target!!.latitude, target.longitude)
            mapCenter = targetP
            binding.mapView.controller.setCenter(targetP)
            recenterMarkerToPosition(targetP)
            moveCameraToPosition(targetP)
        } else if (
            locationManager.isGPSProviderEnabled() || locationManager.isNetworkProviderEnabled()
        ) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
            setProgressBarVisibility(true)
        } else {
            locationPermissionsHelper.showLocationOffDialog(
                requireActivity(),
                R.string.ask_to_turn_location_on_text
            )
        }
        presenter?.onMapReady(exploreMapController)
        registerUnregisterLocationListener(false)
    }

    fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
        MapUtils.registerUnregisterLocationListener(removeLocationListener, locationManager, this)
    }

    override fun recenterMap(currentLatLng: LatLng?) {
        if (isPermissionDenied) {
            if (locationPermissionsHelper.checkLocationPermission(requireActivity())) {
                isPermissionDenied = false
                recenterMap(currentLatLng)
            } else {
                askForLocationPermission()
            }
        } else {
            if (!locationPermissionsHelper.checkLocationPermission(requireActivity())) {
                askForLocationPermission()
            } else {
                locationPermissionGranted()
            }
        }

        if (currentLatLng == null) {
            recenterToUserLocation = true
            return
        }

        recenterMarkerToPosition(GeoPoint(currentLatLng.latitude, currentLatLng.longitude))
        binding.mapView.controller.animateTo(
            GeoPoint(currentLatLng.latitude, currentLatLng.longitude)
        )

        lastMapFocus?.let {
            val myLocation = Location("").apply {
                latitude = it.latitude
                longitude = it.longitude
            }
            val destLocation = Location("").apply {
                latitude = binding.mapView.mapCenter.latitude
                longitude = binding.mapView.mapCenter.longitude
            }
            val distance = myLocation.distanceTo(destLocation)

            if (isNetworkConnectionEstablished()) {
                setSearchThisAreaButtonVisibility(distance > 2000.0)
            } else {
                setSearchThisAreaButtonVisibility(false)
            }
        } ?: setSearchThisAreaButtonVisibility(false)
    }

    override fun hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private fun passInfoToSheet(place: Place) {
        binding.bottomSheetDetailsBinding.directionsButton.setOnClickListener {
            Utils.handleGeoCoordinates(activity, place.location, binding.mapView.zoomLevelDouble)
        }

        binding.bottomSheetDetailsBinding.commonsButton.visibility =
            if (place.hasCommonsLink()) View.VISIBLE else View.GONE

        binding.bottomSheetDetailsBinding.commonsButton.setOnClickListener {
            Utils.handleWebUrl(context, place.siteLinks.commonsLink)
        }

        mediaList?.indexOfFirst { it.filename == place.name }.takeIf { it!! >= 0 }?.let { index ->
            binding.bottomSheetDetailsBinding.mediaDetailsButton.setOnClickListener {
                (parentFragment as? ExploreMapRootFragment)?.onMediaClicked(index)
            }
        }

        binding.bottomSheetDetailsBinding.title.text = place.name.substring(
            5,
            place.name.lastIndexOf(".")
        )
        binding.bottomSheetDetailsBinding.category.text = place.distance

        var descriptionText = place.longDescription.replace("${place.name} (", "")
        descriptionText = if (descriptionText == place.longDescription) descriptionText
        else descriptionText.dropLast(1)

        binding.bottomSheetDetailsBinding.description.text = descriptionText
    }

    override fun addSearchThisAreaButtonAction() {
        binding.searchThisAreaButton.setOnClickListener { presenter?.onSearchThisAreaClicked() }
    }

    override fun setSearchThisAreaButtonVisibility(isVisible: Boolean) {
        binding.searchThisAreaButton.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun setProgressBarVisibility(isVisible: Boolean) {
        binding.mapProgressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun isDetailsBottomSheetVisible(): Boolean {
        return binding.bottomSheetDetailsBinding.root.visibility == View.VISIBLE
    }

    override fun isSearchThisAreaButtonVisible(): Boolean {
        return binding.bottomSheetDetailsBinding.root.visibility == View.VISIBLE
    }

    override fun getLastLocation(): LatLng {
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastLocation()
        }
        return lastKnownLocation!!
    }

    override fun disableFABRecenter() {
        binding.fabRecenter.isEnabled = false
    }

    override fun enableFABRecenter() {
        binding.fabRecenter.isEnabled = true
    }

    /**
     * Adds markers to the map based on the list of NearbyBaseMarker.
     *
     * @param nearbyBaseMarkers The NearbyBaseMarker object representing the markers to be added.
     */
    override fun addMarkersToMap(nearbyBaseMarkers: List<BaseMarker>) {
        clearAllMarkers()
        nearbyBaseMarkers.forEach { addMarkerToMap(it) }
        binding.mapView.invalidate()
    }

    /**
     * Adds a marker to the map based on the specified NearbyBaseMarker.
     *
     * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be added.
     */
    private fun addMarkerToMap(nearbyBaseMarker: BaseMarker) {
        if (isAttachedToActivity()) {
            val items = ArrayList<OverlayItem>()
            val icon = nearbyBaseMarker.icon
            val drawable = BitmapDrawable(resources, icon)
            val point = GeoPoint(
                nearbyBaseMarker.place.location.latitude,
                nearbyBaseMarker.place.location.longitude
            )
            val item = OverlayItem(nearbyBaseMarker.place.name, null, point).apply {
                setMarker(drawable)
            }
            items.add(item)

            val overlay = ItemizedOverlayWithFocus(items, object : OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    val place = nearbyBaseMarker.place
                    clickedMarker?.let {
                        removeMarker(it)
                        addMarkerToMap(it)
                        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                    clickedMarker = nearbyBaseMarker
                    passInfoToSheet(place)
                    return true
                }

                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    return false
                }
            }, context)

            overlay.setFocusItemsOnTap(true)
            binding.mapView.overlays.add(overlay) // Add the overlay to the map
        }
    }

    private fun removeMarker(nearbyBaseMarker: BaseMarker) {
        val place = nearbyBaseMarker.place
        val overlays = binding.mapView.overlays
        var item: ItemizedOverlayWithFocus<OverlayItem>

        for (i in overlays.indices) {
            if (overlays[i] is ItemizedOverlayWithFocus<*>) {
                item = overlays[i] as ItemizedOverlayWithFocus<OverlayItem>
                val overlayItem = item.getItem(0)

                if (place.location.latitude == overlayItem.point.latitude &&
                    place.location.longitude == overlayItem.point.longitude
                ) {
                    binding.mapView.overlays.removeAt(i)
                    binding.mapView.invalidate()
                    break
                }
            }
        }
    }

    override fun clearAllMarkers() {
        if (isAttachedToActivity()) {
            binding.mapView.overlayManager.clear()
            mapCenter?.let { geoPoint ->
                val overlays = binding.mapView.overlays
                val diskOverlay = ScaleDiskOverlay(
                    context, geoPoint, 2000, GeoConstants.UnitOfMeasure.foot
                ).apply {
                    val circlePaint = Paint().apply {
                        color = Color.rgb(128, 128, 128)
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    setCirclePaint2(circlePaint)

                    val diskPaint = Paint().apply {
                        color = Color.argb(40, 128, 128, 128)
                        style = Paint.Style.FILL_AND_STROKE
                    }
                    setCirclePaint1(diskPaint)

                    setDisplaySizeMin(900)
                    setDisplaySizeMax(1700)
                }
                binding.mapView.overlays.add(diskOverlay)

                val startMarker = Marker(binding.mapView).apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.current_location_marker
                    )
                    title = "Your Location"
                    textLabelFontSize = 24
                }
                binding.mapView.overlays.add(startMarker)
            }

            val scaleBarOverlay = ScaleBarOverlay(binding.mapView).apply {
                setScaleBarOffset(15, 25)
                setBackgroundPaint(
                    Paint().apply {
                        setARGB(200, 255, 250, 250)
                    }
                )
                enableScaleBar()
            }
            binding.mapView.overlays.add(scaleBarOverlay)

            binding.mapView.overlays.add(object : MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    clickedMarker?.let {
                        removeMarker(it)
                        addMarkerToMap(it)
                        binding.mapView.invalidate()
                    } ?: Timber.e("CLICKED MARKER IS NULL")

                    if (bottomSheetDetailsBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    } else if (isDetailsBottomSheetVisible()) {
                        hideBottomDetailsSheet()
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean {
                    return false
                }
            }) {})

            binding.mapView.setMultiTouchControls(true)
        }
    }

    private fun recenterMarkerToPosition(geoPoint: GeoPoint?) {
        geoPoint?.let {
            binding.mapView.controller.setCenter(it)
            val overlays = binding.mapView.overlays
            overlays.removeAll { overlay -> overlay is Marker || overlay is ScaleDiskOverlay }

            val diskOverlay = ScaleDiskOverlay(
                context, it, 2000, GeoConstants.UnitOfMeasure.foot
            ).apply {
                val circlePaint = Paint().apply {
                    color = Color.rgb(128, 128, 128)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                setCirclePaint2(circlePaint)

                val diskPaint = Paint().apply {
                    color = Color.argb(40, 128, 128, 128)
                    style = Paint.Style.FILL_AND_STROKE
                }
                setCirclePaint1(diskPaint)

                setDisplaySizeMin(900)
                setDisplaySizeMax(1700)
            }
            binding.mapView.overlays.add(diskOverlay)

            val startMarker = Marker(binding.mapView).apply {
                position = it
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.current_location_marker
                )
                title = "Your Location"
                textLabelFontSize = 24
            }
            binding.mapView.overlays.add(startMarker)
        }
    }

    private fun moveCameraToPosition(geoPoint: GeoPoint) {
        binding.mapView.controller.animateTo(geoPoint)
    }

    private fun moveCameraToPosition(geoPoint: GeoPoint, zoom: Double, speed: Long) {
        binding.mapView.controller.animateTo(geoPoint, zoom, speed)
    }

    override fun getLastMapFocus(): LatLng {
        return lastMapFocus?.let {
            LatLng(it.latitude, it.longitude, 100f)
        } ?: getMapCenter()
    }

    override fun getMapCenter(): LatLng {
        var latLng: LatLng? = null

        if (mapCenter != null) {
            latLng = LatLng(mapCenter!!.latitude, mapCenter!!.longitude, 100f)
        } else {
            applicationKvStore.getString("LastLocation")?.let { lastLocation ->
                val locationLatLng = lastLocation.split(",").map { it.toDouble() }
                lastKnownLocation = LatLng(locationLatLng[0], locationLatLng[1], 1f)
                latLng = lastKnownLocation
            } ?: run {
                latLng = LatLng(51.506255446947776, -0.07483536015053005, 1f)
            }
        }

        if (!isCameFromNearbyMap()) {
            moveCameraToPosition(GeoPoint(latLng?.latitude!!, latLng?.longitude!!))
        }
        return latLng!!
    }

    override fun getMapFocus(): LatLng {
        return LatLng(
            binding.mapView.mapCenter.latitude,
            binding.mapView.mapCenter.longitude,
            100f
        )
    }

    override fun setFABRecenterAction(onClickListener: View.OnClickListener) {
        binding.fabRecenter.setOnClickListener(onClickListener)
    }

    override fun backButtonClicked(): Boolean {
        return if (bottomSheetDetailsBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetDetailsBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            true
        } else {
            false
        }
    }

    private fun initNetworkBroadCastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                activity?.let {
                    if (NetworkUtils.isInternetConnectionEstablished(it)) {
                        if (isNetworkErrorOccurred) {
                            presenter?.updateMap(
                                LocationServiceManager
                                    .LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
                            )
                            isNetworkErrorOccurred = false
                        }
                        snackbar?.dismiss()
                        snackbar = null
                    } else {
                        if (snackbar == null) {
                            snackbar = Snackbar.make(
                                requireView(),
                                R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE
                            )
                            setSearchThisAreaButtonVisibility(false)
                            setProgressBarVisibility(false)
                        }
                        isNetworkErrorOccurred = true
                        snackbar?.show()
                    }
                }
            }
        }
    }

    fun isAttachedToActivity(): Boolean {
        return isVisible && activity != null
    }

    override fun onLocationPermissionDenied(toastMessage: String) {}

    override fun onLocationPermissionGranted() {}
}
