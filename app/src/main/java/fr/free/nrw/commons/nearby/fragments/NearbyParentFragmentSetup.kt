package fr.free.nrw.commons.nearby.fragments

import android.Manifest.permission
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetBehavior
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.isMonumentsEnabled
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import timber.log.Timber
import java.io.IOException

/**
 * Orchestrates initialization of all helpers and UI view components in onViewCreated.
 */
fun NearbyParentFragment.initializeHelpersAndViews() {
    // Initialize the launcher in the appropriate lifecycle method (e.g., onViewCreated)
    inAppCameraLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val areAllGranted = result.values.all { it }
            if (areAllGranted) {
                controller?.locationPermissionCallback?.onLocationPermissionGranted()
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    controller?.handleShowRationaleFlowCameraLocation(
                        requireActivity(),
                        inAppCameraLocationPermissionLauncher,
                        cameraPickLauncherForResult,
                    )
                } else {
                    controller?.locationPermissionCallback?.onLocationPermissionDenied(
                        getString(fr.free.nrw.commons.R.string.in_app_camera_location_permission_denied),
                    )
                }
            }
        }
    isDarkTheme = systemThemeUtils?.isDeviceInNightMode() == true
    if (isMonumentsEnabled) {
        binding?.rlContainerWlmMonthMessage?.visibility = View.VISIBLE
    } else {
        binding?.rlContainerWlmMonthMessage?.visibility = View.GONE
    }
    locationPermissionsHelper =
        locationManager?.let { LocationPermissionsHelper(requireActivity(), it, this) }

    // Set up the floating activity button to toggle the visibility of the legend
    binding?.fabLegend?.setOnClickListener {
        if (binding?.nearbyLegendLayout?.root?.visibility == View.VISIBLE) {
            binding?.nearbyLegendLayout?.root?.visibility = View.GONE
        } else {
            binding?.nearbyLegendLayout?.root?.visibility = View.VISIBLE
        }
    }

    arguments?.let {
        isDepictsPickerMode = it.getBoolean(NearbyParentFragment.ARG_PICKER_MODE, false)
        if (isDepictsPickerMode) {
            viewportHelper.setupDepictsPickerMode(it.getParcelableArrayList(NearbyParentFragment.ARG_PHOTO_LATLNGS))
            locationHelper.setMapCenter(viewportHelper.lastMapFocus)
        }
    }

    presenter?.attachView(this)
    initAllHelpers()
    locationHelper.resetPermissionDenied()
    locationHelper.setRecenterToUserLocation(false)
    initThemePreferences()
    initViews()
    presenter?.setActionListeners(applicationKvStore)
    presenter?.removeOldCachedPlaces(applicationKvStore)
    setupOsmConfiguration()
}

/**
 * Orchestrates OpenStreetMap tile source and attribution configuration.
 */
internal fun NearbyParentFragment.setupOsmConfiguration() {
    setupOsmMapAndTiles()
    setupOsmAttributionAndOverlays()
}

/**
 * Configures tile source, referer header, scale bar, and zoom controls.
 */
internal fun NearbyParentFragment.setupOsmMapAndTiles() {
    org.osmdroid.config.Configuration
        .getInstance()
        .load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))

    // Use the Wikimedia tile server, rather than OpenStreetMap (Mapnik)
    binding?.map?.setTileSource(TileSourceFactory.WIKIMEDIA)
    binding?.map?.setTilesScaledToDpi(true)

    // Add referer HTTP header because the Wikimedia tile server requires it.
    org.osmdroid.config.Configuration
        .getInstance()
        .getAdditionalHttpRequestProperties()
        .put("Referer", "http://maps.wikimedia.org/")

    // Checking for last searched location
    if (viewportHelper.lastMapFocus == null && applicationKvStore?.getString("LastLocation") != null) {
        val locationLatLng = applicationKvStore.getString("LastLocation")!!.split(",")
        viewportHelper.setLastMapFocus(GeoPoint(locationLatLng[0].toDouble(), locationLatLng[1].toDouble()))
    } else if (viewportHelper.lastMapFocus == null) {
        viewportHelper.setLastMapFocus(
            GeoPoint(
                NearbyParentFragment.DEFAULT_LATITUDE,
                NearbyParentFragment.DEFAULT_LONGITUDE,
            ),
        )
    }

    val scaleBarOverlay = ScaleBarOverlay(binding?.map)
    scaleBarOverlay.setScaleBarOffset(NearbyParentFragment.SCALE_BAR_OFFSET_X, NearbyParentFragment.SCALE_BAR_OFFSET_Y)
    val barPaint =
        Paint().apply {
            setARGB(
                NearbyParentFragment.SCALE_BAR_BACKGROUND_ALPHA,
                NearbyParentFragment.SCALE_BAR_BACKGROUND_RED,
                NearbyParentFragment.SCALE_BAR_BACKGROUND_GREEN,
                NearbyParentFragment.SCALE_BAR_BACKGROUND_BLUE,
            )
        }
    scaleBarOverlay.setBackgroundPaint(barPaint)
    scaleBarOverlay.enableScaleBar()
    binding?.map?.overlays?.add(scaleBarOverlay)
    binding?.map?.zoomController?.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    binding?.map?.controller?.setZoom(NearbyParentFragment.ZOOM_LEVEL.toInt())

    // if we came from Explore map using 'Show in Nearby', load Explore map camera position
    if (viewportHelper.isCameFromExploreMap()) {
        moveCameraToPosition(
            viewportHelper.getExploreMapGeoPoint(),
            viewportHelper.getExploreMapZoom(),
            1L,
        )
    }
    binding?.map?.overlays?.add(mapEventsOverlay)
}

/**
 * Configures map listeners, attribution link movement, depicts mode views, and location checks.
 */
internal fun NearbyParentFragment.setupOsmAttributionAndOverlays() {
    binding?.map?.addMapListener(
        object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                presenter?.handleMapScrolled(scope, !statusHelper.isNetworkErrorOccurred)
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean = false
        },
    )

    binding?.map?.setMultiTouchControls(true)
    nearbyParentFragmentInstanceReadyCallback?.onReady()
    initFilterHelper()
    if (!viewportHelper.isCameFromExploreMap()) {
        moveCameraToPosition(viewportHelper.lastMapFocus!!)
    }
    initRvNearbyList()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        binding?.tvAttribution?.text =
            Html.fromHtml(
                getString(fr.free.nrw.commons.R.string.map_attribution),
                Html.FROM_HTML_MODE_LEGACY,
            )
    } else {
        @Suppress("DEPRECATION")
        binding?.tvAttribution?.text =
            Html.fromHtml(getString(fr.free.nrw.commons.R.string.map_attribution))
    }
    binding?.tvAttribution?.movementMethod = LinkMovementMethod.getInstance()
    setupAdvancedQueryView()

    if (isDepictsPickerMode) {
        // hide fab components if depicts picker mode
        binding?.fabCamera?.visibility = View.GONE
        binding?.fabPlus?.visibility = View.GONE
        binding?.fabLegend?.visibility = View.GONE
        binding?.nearbyFilter?.root?.visibility = View.GONE
        binding?.rlContainerWlmMonthMessage?.visibility = View.GONE
        binding?.bottomSheetNearby?.root?.visibility = View.GONE
    }

    if (locationPermissionsHelper?.checkLocationPermission(requireActivity()) != true) {
        askForLocationPermission()
    }
    applyNearbyFabLayout()
}

/**
 * Configures advanced query editor fragment and Learn More link listener.
 */
internal fun NearbyParentFragment.setupAdvancedQueryView() {
    if (isDepictsPickerMode) return
    binding?.nearbyFilterList?.btnAdvancedOptions?.setOnClickListener {
        binding
            ?.nearbyFilter
            ?.searchViewLayout
            ?.searchView
            ?.clearFocus()
        showHideAdvancedQueryFragment(true)
        val fragment = AdvanceQueryFragment()
        val bundle = Bundle()
        try {
            bundle.putString(
                "query",
                FileUtils.readFromResource("/queries/radius_query_for_upload_wizard.rq"),
            )
        } catch (e: IOException) {
            Timber.e(e)
        }
        fragment.arguments = bundle
        fragment.callback =
            object : AdvanceQueryFragment.Callback {
                override fun close() {
                    // Back should first hide the bottom sheet if it is expanded
                    showHideAdvancedQueryFragment(false)
                }

                override fun reset() {
                    resetAdvancedQuery()
                }

                override fun apply(query: String) {
                    presenter?.setAdvancedQuery(query)
                    presenter?.updateMapAndList(LocationChangeType.CUSTOM_QUERY)
                    showHideAdvancedQueryFragment(false)
                }
            }
        childFragmentManager
            .beginTransaction()
            .replace(fr.free.nrw.commons.R.id.fl_container_nearby_children, fragment)
            .commit()
    }

    binding?.tvLearnMore?.setOnClickListener {
        onLearnMoreClicked()
    }
}

/**
 * Initializes all helper delegates sequentially.
 */
internal fun NearbyParentFragment.initAllHelpers() {
    initFileExportHelper()
    initMarkerOverlayHelper()
    initLocationHelper()
    initViewportHelper()
    initBottomSheetHelper()
    initFabHelper()
    initFilterHelper()
    initMediaDetailHelper()
    initPlacesQueryHelper()
    initStatusHelper()
}

/**
 * Initializes file export helper for KML and GPX persistence.
 */
internal fun NearbyParentFragment.initFileExportHelper() {
    fileExportHelper.init(
        activityProvider = { activity },
        nearbyControllerProvider = { nearbyController },
        compositeDisposableProvider = { disposables },
        mapFocusProvider = { mapFocus },
        viewCallback =
            object : NearbyFileExportHelper.ExportViewCallback {
                override fun showProgressDialog() {
                    progressDialog?.show()
                }

                override fun hideProgressDialog() {
                    progressDialog?.hide()
                }

                override fun showErrorMessage(message: String) {
                    this@initFileExportHelper.showErrorMessage(message)
                }

                override fun showToast(messageResId: Int) {
                    Toast.makeText(context, getString(messageResId), Toast.LENGTH_SHORT).show()
                }

                override fun setProgressBarVisibility(isVisible: Boolean) {
                    this@initFileExportHelper.setProgressBarVisibility(isVisible)
                }

                override fun lockUnlockNearby(isLocked: Boolean) {
                    presenter?.lockUnlockNearby(isLocked)
                }

                override fun setFilterState() {
                    this@initFileExportHelper.setFilterState()
                }
            },
    )
}

/**
 * Initializes marker overlay helper with map providers and marker click callbacks.
 */
internal fun NearbyParentFragment.initMarkerOverlayHelper() {
    markerOverlayHelper.init(
        mapViewProvider = { binding?.map },
        contextProvider = { context },
        locationManagerProvider = { locationManager },
        mapStateProvider =
            object : NearbyMarkerOverlayHelper.MapStateProvider {
                override fun getMapCenter(): GeoPoint? = locationHelper.mapCenter

                override fun getMapEventsOverlay(): MapEventsOverlay? = mapEventsOverlay

                override fun isPermissionDenied(): Boolean = locationHelper.isPermissionDenied

                override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                override fun getAllPhotoLatLngs(): List<LatLng>? = viewportHelper.allPhotoLatLngs

                override fun getNearestPlace(): Place? = viewportHelper.nearestPlace
            },
        markerCallback =
            object : NearbyMarkerOverlayHelper.MarkerCallback {
                override fun onMarkerClicked(
                    place: Place,
                    marker: Marker,
                    isBookmarked: Boolean,
                ) {
                    if (!statusHelper.isNetworkErrorOccurred) {
                        binding?.bottomSheetDetails?.dataCircularProgress?.visibility =
                            View.VISIBLE
                        binding?.bottomSheetDetails?.icon?.visibility = View.GONE
                        binding?.bottomSheetDetails?.wikiDataLl?.visibility = View.GONE
                        if (place.name == "") {
                            getPlaceData(place, marker, isBookmarked)
                        } else {
                            marker.showInfoWindow()
                            binding?.bottomSheetDetails?.dataCircularProgress?.visibility =
                                View.GONE
                            binding?.bottomSheetDetails?.icon?.visibility = View.VISIBLE
                            binding?.bottomSheetDetails?.wikiDataLl?.visibility = View.VISIBLE
                            bottomSheetHelper.passInfoToSheet(place)
                            bottomSheetHelper.hideBottomSheet()
                        }
                        bottomSheetHelper.bottomSheetDetailsBehavior?.state =
                            BottomSheetBehavior.STATE_COLLAPSED
                    } else {
                        marker.showInfoWindow()
                    }
                }

                override fun onPhotoMarkerClicked(marker: Marker) {
                    bottomSheetHelper.hideBottomDetailsSheet()
                }

                override fun onBoundingBoxZoomCalculated(boundingBox: BoundingBox) {
                    viewportHelper.setPhotoPinBoundingBox(boundingBox)
                    // only if multiple pins are present
                    if (viewportHelper.needsZoomOut && (viewportHelper.allPhotoLatLngs?.size ?: 0) > 1) {
                        val preferredZoom =
                            if (viewportHelper.getExploreMapZoom() > 0.0) {
                                viewportHelper.getExploreMapZoom()
                            } else {
                                NearbyParentFragment.ZOOM_LEVEL.toDouble()
                            }
                        // Calculate what zoom the bounding box WOULD use
                        // BoundingBox might zoom out too far if pins are situated far apart ,
                        // which makes the query very slow. so we restrict it
                        val boundingBoxZoom =
                            binding?.map?.calculateBoundingBoxZoom(
                                boundingBox,
                                NearbyParentFragment.BOUNDING_BOX_BORDER_SIZE_PX,
                            ) ?: preferredZoom
                        if (boundingBoxZoom < preferredZoom) {
                            binding?.map?.controller?.setZoom(preferredZoom)
                            binding?.map?.controller?.animateTo(viewportHelper.lastMapFocus)
                        } else {
                            binding?.map?.zoomToBoundingBox(
                                boundingBox,
                                true,
                                NearbyParentFragment.BOUNDING_BOX_BORDER_SIZE_PX,
                            )
                        }
                        viewportHelper.setNeedsZoomOut(false)
                    }
                }
            },
    )
}

/**
 * Initializes location helper with GPS manager and location permission actions.
 */
internal fun NearbyParentFragment.initLocationHelper() {
    locationHelper.init(
        activityProvider = { activity },
        locationManagerProvider = { locationManager },
        locationPermissionsHelperProvider = { locationPermissionsHelper },
        applicationKvStoreProvider = { applicationKvStore },
        actions =
            object : NearbyLocationHelper.NearbyLocationActions {
                override fun onMapReady() {
                    presenter?.onMapReady()
                }

                override fun onUpdateMapAndList(locationChangeType: LocationChangeType) {
                    presenter?.updateMapAndList(locationChangeType)
                }

                override fun onCameraCenterRequested(targetP: GeoPoint) {
                    binding?.map?.controller?.setCenter(targetP)
                }

                override fun onUserLocationOverlaysUpdated(
                    targetP: GeoPoint,
                    invalidate: Boolean,
                ) {
                    updateUserLocationOverlays(targetP, invalidate)
                }

                override fun onCameraMoveRequested(targetP: GeoPoint) {
                    moveCameraToPosition(targetP)
                }

                override fun isCameFromExploreMap(): Boolean = viewportHelper.isCameFromExploreMap()

                override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                override fun setProgressBarVisibility(isVisible: Boolean) {
                    this@initLocationHelper.setProgressBarVisibility(isVisible)
                }

                override fun showLocationOffDialog() {
                    activity?.let {
                        locationPermissionsHelper?.showLocationOffDialog(
                            it,
                            fr.free.nrw.commons.R.string.ask_to_turn_location_on_text,
                        )
                    }
                }

                override fun askForLocationPermission() {
                    locationPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
                }
            },
    )
}

/**
 * Initializes viewport helper for camera movements and recentering.
 */
internal fun NearbyParentFragment.initViewportHelper() {
    viewportHelper.init(
        mapViewProvider = { binding?.map },
        activityProvider = { activity },
        actions =
            object : NearbyViewportHelper.NearbyViewportActions {
                override fun onPlaceHighlighted(place: Place) {
                    viewportHelper.setNearestPlace(place)
                    highlightNearestPlace(place)
                }

                override fun onRecenterMapRequested(currentLatLng: LatLng?) {
                    recenterMap(currentLatLng)
                }
            },
    )
}
