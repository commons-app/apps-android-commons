package fr.free.nrw.commons.nearby.fragments

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.appcompat.queryTextChanges
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.MapController.NearbyPlacesInfo
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.contributions.loadExploreMapFromNearby
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaDetailPagerFragment
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.navtab.NavTab
import fr.free.nrw.commons.nearby.BottomSheetAdapter
import fr.free.nrw.commons.nearby.BottomSheetAdapter.ItemClickListener
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.PlaceTypeLabelsClient
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyFilterSearchRecyclerViewAdapter
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.NearbyUtil
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.WikidataFeedback
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.model.BottomSheetItem
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ExecutorUtils.get
import fr.free.nrw.commons.utils.LayoutUtils.getScreenWidth
import fr.free.nrw.commons.utils.LayoutUtils.setLayoutHeightAlignedToWidth
import fr.free.nrw.commons.utils.MapUtils.NETWORK_INTENT_ACTION
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToBigFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToSmallFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.removeAnchorFromFAB
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.utils.isMonumentsEnabled
import fr.free.nrw.commons.wikidata.WikidataConstants
import fr.free.nrw.commons.wikidata.WikidataEditListener
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.TilesOverlay
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.concurrent.Volatile

class NearbyParentFragment(
    val fileExportHelper: NearbyFileExportHelper = NearbyFileExportHelper(),
    val markerOverlayHelper: NearbyMarkerOverlayHelper = NearbyMarkerOverlayHelper(),
    val bottomSheetHelper: NearbyBottomSheetHelper = NearbyBottomSheetHelper(),
    val locationHelper: NearbyLocationHelper = NearbyLocationHelper(),
    val viewportHelper: NearbyViewportHelper = NearbyViewportHelper(),
) : CommonsDaggerSupportFragment(),
    NearbyParentFragmentContract.View,
    WikidataP18EditListener,
    LocationUpdateListener,
    LocationPermissionCallback,
    ItemClickListener,
    MediaDetailProvider {
    var binding: FragmentNearbyParentBinding? = null

    /** Filter helper instance. */
    @VisibleForTesting
    var filterHelper: NearbyFilterHelper = NearbyFilterHelper()

    /** FAB helper instance. */
    @VisibleForTesting
    var fabHelper: NearbyFabHelper = NearbyFabHelper()

    val mapEventsOverlay: MapEventsOverlay =
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (clickedMarker != null) {
                        clickedMarker!!.closeInfoWindow()
                    } else {
                        Timber.e("CLICKED MARKER IS NULL")
                    }
                    if (isListBottomSheetExpanded) {
                        // Back should first hide the bottom sheet if it is expanded
                        hideBottomSheet()
                    } else if (isDetailsBottomSheetVisible) {
                        hideBottomDetailsSheet()
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            },
        )

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var nearbyController: NearbyController

    @Inject
    @Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var mediaClient: MediaClient

    lateinit var mediaDetails: MediaDetailPagerFragment

    lateinit var media: Media

    @Inject
    lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Inject
    lateinit var placesRepository: PlacesRepository

    @Inject
    lateinit var controller: ContributionController

    @Inject
    lateinit var wikidataEditListener: WikidataEditListener

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    @Inject
    lateinit var commonPlaceClickActions: CommonPlaceClickActions

    @Inject
    lateinit var placeTypeLabelsClient: PlaceTypeLabelsClient

    private var locationPermissionsHelper: LocationPermissionsHelper? = null
    /** Adapter for filter list forwarded to [filterHelper] for testing. */
    @VisibleForTesting
    var nearbyFilterSearchRecyclerViewAdapter: NearbyFilterSearchRecyclerViewAdapter?
        get() = filterHelper.nearbyFilterSearchRecyclerViewAdapter
        set(value) {
            filterHelper.nearbyFilterSearchRecyclerViewAdapter = value
        }
    /** Backing behavior for places list bottom sheet, delegating to [bottomSheetHelper] for testing. */
    @VisibleForTesting
    var bottomSheetListBehavior: BottomSheetBehavior<*>? = null
        get() = bottomSheetHelper.bottomSheetListBehavior ?: field
        set(value) {
            field = value
            bottomSheetHelper.bottomSheetListBehavior = value
        }

    /** Backing behavior for place details bottom sheet, delegating to [bottomSheetHelper] for testing. */
    @VisibleForTesting
    var bottomSheetDetailsBehavior: BottomSheetBehavior<*>? = null
        get() = bottomSheetHelper.bottomSheetDetailsBehavior ?: field
        set(value) {
            field = value
            bottomSheetHelper.bottomSheetDetailsBehavior = value
        }
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isNetworkErrorOccurred = false
    private var snackbar: Snackbar? = null
    private var hasShownOfflinePinsMessage = false
    private var view: View? = null
    private var scope: LifecycleCoroutineScope? = null
    private var presenter: NearbyParentFragmentPresenter? = null

    @Suppress("ktlint:standard:backing-property-naming")
    private var _isDarkTheme = false
    /** FAB expansion state forwarded to [fabHelper] for testing. */
    @VisibleForTesting
    var isFABsExpanded: Boolean
        get() = fabHelper.isFABsExpanded
        set(value) {
            fabHelper.isFABsExpanded = value
        }
    /** Selected place forwarded to [bottomSheetHelper] for testing. */
    @VisibleForTesting
    var selectedPlace: Place?
        get() = bottomSheetHelper.selectedPlace
        set(value) {
            bottomSheetHelper.selectedPlace = value
        }
    private val clickedMarker: Marker?
    get() = markerOverlayHelper.clickedMarker
    private var progressDialog: ProgressDialog? = null

    /** Permission state forwarded to [locationHelper] for testing. */
    @VisibleForTesting
    var isPermissionDenied: Boolean
        get() = locationHelper.isPermissionDenied
        set(value) {
            locationHelper.isPermissionDenied = value
        }

    /** Recenter to user location flag forwarded to [locationHelper] for testing. */
    @VisibleForTesting
    var recenterToUserLocation: Boolean
        get() = locationHelper.recenterToUserLocation
        set(value) {
            locationHelper.recenterToUserLocation = value
        }

    /** Map center coordinate forwarded to [locationHelper] for testing. */
    @VisibleForTesting
    var mapCenter: GeoPoint?
        get() = locationHelper.mapCenter
        set(value) {
            locationHelper.mapCenter = value
        }

    var intentFilter: IntentFilter = IntentFilter(NETWORK_INTENT_ACTION)

    /** Last place centered forwarded to [viewportHelper] for testing. */
    @VisibleForTesting
    var lastPlaceToCenter: Place?
        get() = viewportHelper.lastPlaceToCenter
        set(value) {
            viewportHelper.lastPlaceToCenter = value
        }

    /** Last known location coordinates forwarded to [locationHelper] for testing. */
    @VisibleForTesting
    var lastKnownLocation: LatLng?
        get() = locationHelper.lastKnownLocation
        set(value) {
            locationHelper.lastKnownLocation = value
        }

    private var isVisibleToUser = false
    private var lastFocusLocation: LatLng? = null
    private var adapter: PlaceAdapter? = null

    /** Last map focus coordinate forwarded to [viewportHelper] for testing. */
    @VisibleForTesting
    var lastMapFocus: GeoPoint?
        get() = viewportHelper.lastMapFocus
        set(value) {
            viewportHelper.lastMapFocus = value
        }
    private var nearbyParentFragmentInstanceReadyCallback: NearbyParentFragmentInstanceReadyCallback? =
        null
    private var nearestPlace: Place? = null
    private var isDepictsPickerMode: Boolean = false
    private var allPhotoLatLngs: ArrayList<LatLng>? = null
    private var photoPinBoundingBox: BoundingBox? = null
    private var needsZoomOut: Boolean = true

    @Volatile
    private var stopQuery = false

    // Explore map data (for if we came from Explore)
    private var prevZoom = 0.0
    private var prevLatitude = 0.0
    private var prevLongitude = 0.0

    private val searchHandler = Handler()
    private val searchRunnable: Runnable? = null

    /** Action items list forwarded to [bottomSheetHelper] for testing. */
    @VisibleForTesting
    var dataList: MutableList<BottomSheetItem>?
        get() = bottomSheetHelper.dataList
        set(value) {
            bottomSheetHelper.dataList = value
        }

    /** Bottom sheet adapter forwarded to [bottomSheetHelper] for testing. */
    @VisibleForTesting
    var bottomSheetAdapter: BottomSheetAdapter?
        get() = bottomSheetHelper.bottomSheetAdapter
        set(value) {
            bottomSheetHelper.bottomSheetAdapter = value
        }

    private val galleryPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        // Handle the result from the gallery
                        controller?.onPictureReturnedFromGallery(
                            result,
                            requireActivity(),
                            callbacks,
                        )
                    }
                },
            )
        }

    private val customSelectorLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        if (result != null) {
                            controller?.onPictureReturnedFromCustomSelector(
                                result,
                                requireActivity(),
                                callbacks,
                            )
                        }
                    }
                },
            )
        }

    private val cameraPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        if (result != null) {
                            controller?.onPictureReturnedFromCamera(
                                result,
                                requireActivity(),
                                callbacks,
                            )
                        }
                    }
                },
            )
        }

    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val locationPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                locationPermissionGranted()
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    showAlertDialog(
                        requireActivity(),
                        getString(fr.free.nrw.commons.R.string.location_permission_title),
                        getString(fr.free.nrw.commons.R.string.location_permission_rationale_nearby),
                        getString(fr.free.nrw.commons.R.string.ok),
                        getString(fr.free.nrw.commons.R.string.cancel),
                        {
                            askForLocationPermission()
                        },
                        null,
                        null,
                    )
                } else {
                    if (isPermissionDenied) {
                        locationPermissionsHelper?.showAppSettingsDialog(
                            requireActivity(),
                            fr.free.nrw.commons.R.string.nearby_needs_location,
                        )
                    }
                    Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                    isPermissionDenied = true
                }
            }
        }

    /**
     * WLM URL
     */
    val wlmUrl =
        "https://commons.wikimedia.org/wiki/Commons:Mobile_app/Contributing_to_WLM_using_the_app"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Label.init(requireContext().applicationContext)
        loadExploreMapData()

        binding = FragmentNearbyParentBinding.inflate(inflater, container, false)
        view = binding!!.root

        initNetworkBroadCastReceiver()
        scope = viewLifecycleOwner.lifecycleScope
        presenter =
            NearbyParentFragmentPresenter(
                bookmarkLocationDao!!,
                placesRepository!!,
                nearbyController!!,
            )
        progressDialog = ProgressDialog(activity)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage("Saving in progress...")
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return view
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        if (isDepictsPickerMode) return

        inflater.inflate(fr.free.nrw.commons.R.menu.nearby_fragment_menu, menu)
        val refreshButton = menu.findItem(fr.free.nrw.commons.R.id.item_refresh)
        val listMenu = menu.findItem(fr.free.nrw.commons.R.id.list_sheet)
        val showInExploreButton = menu.findItem(fr.free.nrw.commons.R.id.list_item_show_in_explore)
        val saveAsGPXButton = menu.findItem(fr.free.nrw.commons.R.id.list_item_gpx)
        val saveAsKMLButton = menu.findItem(fr.free.nrw.commons.R.id.list_item_kml)
        refreshButton.setOnMenuItemClickListener {
            try {
                emptyCache()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            false
        }
        listMenu.setOnMenuItemClickListener {
            listOptionMenuItemClicked()
            false
        }

        showInExploreButton.setOnMenuItemClickListener { item ->
            (context as MainActivity).loadExploreMapFromNearby(
                binding?.map?.zoomLevelDouble ?: 0.0, // Using safe calls to avoid NPE
                binding?.map?.mapCenter?.latitude ?: 0.0,
                binding?.map?.mapCenter?.longitude ?: 0.0,
            )
            return@setOnMenuItemClickListener true
        }

        saveAsGPXButton.setOnMenuItemClickListener {
            try {
                progressDialog!!.setTitle(getString(fr.free.nrw.commons.R.string.saving_gpx_file))
                progressDialog!!.show()
                savePlacesAsGPX()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            false
        }
        saveAsKMLButton.setOnMenuItemClickListener {
            try {
                progressDialog!!.setTitle(getString(fr.free.nrw.commons.R.string.saving_kml_file))
                progressDialog!!.show()
                savePlacesAsKML()
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
            false
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
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
                            inAppCameraLocationPermissionLauncher, // Reference it directly
                            cameraPickLauncherForResult,
                        )
                    } else {
                        controller?.locationPermissionCallback?.onLocationPermissionDenied(
                            getString(fr.free.nrw.commons.R.string.in_app_camera_location_permission_denied),
                        )
                    }
                }
            }
        _isDarkTheme = systemThemeUtils?.isDeviceInNightMode() == true
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
            isDepictsPickerMode = it.getBoolean(ARG_PICKER_MODE, false)
            if (isDepictsPickerMode) {
                allPhotoLatLngs = it.getParcelableArrayList(ARG_PHOTO_LATLNGS)
                if (!allPhotoLatLngs.isNullOrEmpty()) {
                    val first = allPhotoLatLngs!![0]
                    lastMapFocus = GeoPoint(first.latitude, first.longitude)
                    mapCenter = lastMapFocus
                    needsZoomOut = true
                }
            }
        }

        presenter?.attachView(this)
        fileExportHelper.init(
            activityProvider = { activity },
            nearbyControllerProvider = { nearbyController },
            compositeDisposableProvider = { compositeDisposable },
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
                        this@NearbyParentFragment.showErrorMessage(message)
                    }

                    override fun showToast(messageResId: Int) {
                        Toast.makeText(context, getString(messageResId), Toast.LENGTH_SHORT).show()
                    }

                    override fun setProgressBarVisibility(isVisible: Boolean) {
                        this@NearbyParentFragment.setProgressBarVisibility(isVisible)
                    }

                    override fun lockUnlockNearby(isLocked: Boolean) {
                        presenter?.lockUnlockNearby(isLocked)
                    }

                    override fun setFilterState() {
                        this@NearbyParentFragment.setFilterState()
                    }
                },
        )
        markerOverlayHelper.init(
            mapViewProvider = { binding?.map },
            contextProvider = { context },
            locationManagerProvider = { locationManager },
            mapStateProvider =
                object : NearbyMarkerOverlayHelper.MapStateProvider {
                    override fun getMapCenter(): GeoPoint? = mapCenter
                    override fun getMapEventsOverlay(): MapEventsOverlay? = mapEventsOverlay
                    override fun isPermissionDenied(): Boolean = isPermissionDenied
                    override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode
                    override fun getAllPhotoLatLngs(): List<LatLng>? = allPhotoLatLngs
                    override fun getNearestPlace(): Place? = nearestPlace
                },
            markerCallback =
                object : NearbyMarkerOverlayHelper.MarkerCallback {
                    override fun onMarkerClicked(
                        place: Place,
                        marker: Marker,
                        isBookmarked: Boolean,
                    ) {
                        if (!isNetworkErrorOccurred) {
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
                        photoPinBoundingBox = boundingBox
                        // center on average of all photos coordinates and zoom out and run only once
                        // only if multiple pins are present
                        if (needsZoomOut && (allPhotoLatLngs?.size ?: 0) > 1) {
                            val preferredZoom = if (prevZoom > 0.0) prevZoom else ZOOM_LEVEL.toDouble()
                            // Calculate what zoom the bounding box WOULD use
                            val boundingBoxZoom =
                                binding?.map?.calculateBoundingBoxZoom(
                                    boundingBox,
                                    BOUNDING_BOX_BORDER_SIZE_PX,
                                ) ?: preferredZoom
                            // BoundingBox might zoom out too far if pins are situated far apart ,
                            // which makes the query very slow. so we restrict it
                            if (boundingBoxZoom < preferredZoom) {
                                binding?.map?.controller?.setZoom(preferredZoom)
                                binding?.map?.controller?.animateTo(lastMapFocus)
                            } else {
                                binding?.map?.zoomToBoundingBox(
                                    boundingBox,
                                    true,
                                    BOUNDING_BOX_BORDER_SIZE_PX,
                                )
                            }
                            needsZoomOut = false
                        }
                    }
                },
        )
        initLocationHelper()
        initViewportHelper()
        isPermissionDenied = false
        recenterToUserLocation = false
        initThemePreferences()
        initViews()
        presenter?.setActionListeners(applicationKvStore)
        presenter?.removeOldCachedPlaces(applicationKvStore)
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

        if (lastMapFocus == null && applicationKvStore?.getString("LastLocation") != null) {
            // Checking for last searched location
            val locationLatLng = applicationKvStore!!.getString("LastLocation")!!.split(",")
            lastMapFocus = GeoPoint(locationLatLng[0].toDouble(), locationLatLng[1].toDouble())
        } else if (lastMapFocus == null) {
            lastMapFocus = GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        }

        val scaleBarOverlay = ScaleBarOverlay(binding?.map)
        scaleBarOverlay.setScaleBarOffset(SCALE_BAR_OFFSET_X, SCALE_BAR_OFFSET_Y)
        val barPaint =
            Paint().apply {
                setARGB(
                    SCALE_BAR_BACKGROUND_ALPHA,
                    SCALE_BAR_BACKGROUND_RED,
                    SCALE_BAR_BACKGROUND_GREEN,
                    SCALE_BAR_BACKGROUND_BLUE,
                )
            }
        scaleBarOverlay.setBackgroundPaint(barPaint)
        scaleBarOverlay.enableScaleBar()
        binding?.map?.overlays?.add(scaleBarOverlay)
        binding
            ?.map
            ?.getZoomController()
            ?.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding?.map?.controller?.setZoom(ZOOM_LEVEL.toInt())

        // if we came from Explore map using 'Show in Nearby', load Explore map camera position
        if (isCameFromExploreMap()) {
            moveCameraToPosition(
                GeoPoint(prevLatitude, prevLongitude),
                prevZoom,
                1L,
            )
        }
        binding?.map?.getOverlays()?.add(mapEventsOverlay)

        binding?.map?.addMapListener(
            object : MapListener {
                override fun onScroll(event: ScrollEvent): Boolean {
                    presenter?.handleMapScrolled(scope, !isNetworkErrorOccurred)
                    return true
                }

                override fun onZoom(event: ZoomEvent): Boolean = false
            },
        )

        binding?.map?.setMultiTouchControls(true)
        nearbyParentFragmentInstanceReadyCallback?.onReady()
        initFilterHelper()
        if (!isCameFromExploreMap()) {
            moveCameraToPosition(lastMapFocus!!)
        }
        moveCameraToPosition(lastMapFocus!!)
        initRvNearbyList()
        onResume()

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
        if (!isDepictsPickerMode) {
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

        // hide fab components if depicts picker mode
        if (isDepictsPickerMode) {
            binding?.fabCamera?.visibility = View.GONE
            binding?.fabPlus?.visibility = View.GONE
            binding?.fabLegend?.visibility = View.GONE
            binding?.nearbyFilter?.root?.visibility = View.GONE
            binding?.rlContainerWlmMonthMessage?.visibility = View.GONE
            binding?.bottomSheetNearby?.root?.visibility = View.GONE
        }

        if (!locationPermissionsHelper?.checkLocationPermission(requireActivity())!!) {
            askForLocationPermission()
        }
        applyNearbyFabLayout()
    }

    fun addPhotoMarkerIfDepictsMode() {
        markerOverlayHelper.addPhotoMarkerIfDepictsMode()
    }

    /**
     * Fetch Explore map camera data from fragment arguments if any.
     */
    fun loadExploreMapData() {
        // get fragment arguments
        if (arguments != null) {
            prevZoom = requireArguments().getDouble("prev_zoom")
            prevLatitude = requireArguments().getDouble("prev_latitude")
            prevLongitude = requireArguments().getDouble("prev_longitude")
        }
    }

    /**
     * Checks if fragment arguments contain data from Explore map. if present, then the user
     * navigated from Explore using 'Show in Nearby'.
     *
     * @return true if user navigated from Explore map
     */
    fun isCameFromExploreMap(): Boolean = prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0

    /**
     * Initialise background based on theme, this should be doe ideally via styles, that would need
     * another refactor
     */
    private fun initThemePreferences() {
        filterHelper.applyTheme(_isDarkTheme)
        if (_isDarkTheme) {
            binding!!.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                requireContext().resources.getColor(fr.free.nrw.commons.R.color.contributionListDarkBackground),
            )
            binding!!
                .map.overlayManager.tilesOverlay
                .setColorFilter(TilesOverlay.INVERT_COLORS)
        } else {
            binding!!.bottomSheetNearby.rvNearbyList.setBackgroundColor(
                requireContext().resources.getColor(fr.free.nrw.commons.R.color.white),
            )
        }
    }

    private fun initRvNearbyList() {
        binding!!.bottomSheetNearby.rvNearbyList.layoutManager = LinearLayoutManager(context)
        adapter =
            PlaceAdapter(
                bookmarkLocationsDao = bookmarkLocationDao,
                scope = scope,
                onPlaceClicked = { place: Place ->
                    moveCameraToPosition(
                        GeoPoint(
                            place.location.latitude,
                            place.location.longitude,
                        ),
                    )
                },
                onBookmarkClicked = { place: Place?, _: Boolean? ->
                    presenter!!.toggleBookmarkedStatus(place, scope)
                },
                commonPlaceClickActions = commonPlaceClickActions,
                inAppCameraLocationPermissionLauncher = inAppCameraLocationPermissionLauncher,
                galleryPickLauncherForResult = galleryPickLauncherForResult,
                cameraPickLauncherForResult = cameraPickLauncherForResult,
            )
        binding!!.bottomSheetNearby.rvNearbyList.adapter = adapter
    }

    private fun performMapReadyActions() {
        if (!isDepictsPickerMode &&
            activity is MainActivity &&
            (activity as MainActivity).activeFragment == ActiveFragment.NEARBY
        ) {
            if (applicationKvStore!!.getBoolean("doNotAskForLocationPermission", false) &&
                !locationPermissionsHelper!!.checkLocationPermission(requireActivity())
            ) {
                isPermissionDenied = true
            }
        }
        presenter!!.onMapReady()
    }

    override fun askForLocationPermission() {
        locationHelper.askForLocationPermission()
    }

    fun locationPermissionGranted() {
        locationHelper.locationPermissionGranted()
    }

    override fun onResume() {
        super.onResume()
        binding?.map?.onResume()
        clickedMarker?.infoWindow?.isOpen?.let {
            if (!it) {
                bottomSheetHelper.hideBottomDetailsSheet()
            }
        }
        presenter?.attachView(this)
        registerNetworkReceiver()

        binding?.coordinatorLayout?.visibility = View.VISIBLE
        binding?.map?.setMultiTouchControls(true)
        binding?.map?.isClickable = true

        if (isResumed &&
            (activity as? MainActivity)?.activeFragment == ActiveFragment.NEARBY &&
            !isDepictsPickerMode
        ) {
            if (activity?.let { locationPermissionsHelper?.checkLocationPermission(it) } == true) {
                locationPermissionGranted()
            } else {
                startMapWithoutPermission()
            }
        }
    }

    /**
     * Starts the map without GPS and without permission By default it points to DEFAULT_LATITUDE,DEFAULT_LONGITUDE
     * coordinates, other than that it points to the last known location which can be get by the key
     * "LastLocation" from applicationKvStore
     */
    fun startMapWithoutPermission() {
        locationHelper.startMapWithoutPermission()
    }

    private fun registerNetworkReceiver() {
        if (activity != null) {
            requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        }
    }

    override fun onPause() {
        super.onPause()
        binding!!.map.onPause()
        compositeDisposable.clear()
        presenter!!.detachView()
        registerUnregisterLocationListener(true)
        try {
            if (broadcastReceiver != null && activity != null) {
                requireContext().unregisterReceiver(broadcastReceiver)
            }

            if (locationManager != null && presenter != null) {
                locationManager!!.removeLocationListener(presenter!!)
                locationManager!!.unregisterLocationManager()
            }
        } catch (e: Exception) {
            Timber.e(e)
            // Broadcast receivers should always be unregistered inside catch,
            // you never know if they were already registered or not
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        filterHelper.onDestroyView()
        searchRunnable?.let {
            searchHandler.removeCallbacks(it)
        } ?: run {
            Timber.w("NearbyParentFragment: searchRunnable is null")
        }

        if (presenter == null) Timber.w("NearbyParentFragment: presenter is null")
        if (applicationKvStore == null) Timber.w("NearbyParentFragment: applicationKvStore is null")

        presenter?.removeNearbyPreferences(applicationKvStore ?: return)
    }

    private fun initViews() {
        Timber.d("init views called")
        initBottomSheetHelper()
        initFabHelper()
        if (!isMonumentsEnabled) {
            NearbyFilterState.setWlmSelected(false)
        }
    }

    private fun initBottomSheetHelper() {
        bottomSheetHelper.init(
            bindingProvider = { binding },
            contextProvider = { context },
            activityProvider = { activity },
            bookmarkDaoProvider = { bookmarkLocationDao },
            actions =
                object : NearbyBottomSheetHelper.NearbyBottomSheetActions {
                    override fun onCameraCenterRequested(place: Place) {
                        moveCameraToPosition(GeoPoint(place.location.latitude, place.location.longitude))
                    }

                    override fun onMediaClicked(imageName: String) {
                        handleMediaClick(imageName)
                    }

                    override fun onBookmarkToggled(place: Place) {
                        presenter?.toggleBookmarkedStatus(place, scope)
                    }

                    override fun onSheetStateChanged(newState: Int) {
                        prepareViewsForSheetPosition(newState)
                    }

                    override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                    override fun onUploadActionInitiated(
                        place: Place,
                        uploadType: NearbyBottomSheetHelper.UploadType,
                    ) {
                        storeSharedPrefs(place)
                        val act = activity ?: return
                        when (uploadType) {
                            NearbyBottomSheetHelper.UploadType.CAMERA -> {
                                Timber.d("Camera button tapped. Place: %s", place.toString())
                                controller?.initiateCameraPick(
                                    act,
                                    inAppCameraLocationPermissionLauncher,
                                    cameraPickLauncherForResult,
                                )
                            }
                            NearbyBottomSheetHelper.UploadType.GALLERY -> {
                                Timber.d("Gallery button tapped. Place: %s", place.toString())
                                controller?.initiateGalleryPick(
                                    act,
                                    galleryPickLauncherForResult,
                                    false,
                                )
                            }
                            NearbyBottomSheetHelper.UploadType.CUSTOM_GALLERY -> {
                                Timber.d("Gallery button tapped. Place: %s", place.toString())
                                // Pass singleSelection = true for Nearby flow
                                controller?.initiateCustomGalleryPickWithPermission(
                                    act,
                                    customSelectorLauncherForResult,
                                    singleSelection = true,
                                )
                            }
                        }
                    }

                    override fun getCoroutineScope(): LifecycleCoroutineScope? = scope

                    override fun getPlaceAdapter(): PlaceAdapter? = adapter
                },
        )
    }

    private fun initLocationHelper() {
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

                    override fun onUserLocationOverlaysUpdated(targetP: GeoPoint, invalidate: Boolean) {
                        updateUserLocationOverlays(targetP, invalidate)
                    }

                    override fun onCameraMoveRequested(targetP: GeoPoint) {
                        moveCameraToPosition(targetP)
                    }

                    override fun isCameFromExploreMap(): Boolean = this@NearbyParentFragment.isCameFromExploreMap()

                    override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                    override fun setProgressBarVisibility(isVisible: Boolean) {
                        this@NearbyParentFragment.setProgressBarVisibility(isVisible)
                    }

                    override fun showLocationOffDialog() {
                        activity?.let {
                            locationPermissionsHelper?.showLocationOffDialog(
                                it,
                                R.string.ask_to_turn_location_on_text,
                            )
                        }
                    }

                    override fun askForLocationPermission() {
                        locationPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
                    }
                },
        )
    }

    private fun initViewportHelper() {
        viewportHelper.init(
            mapViewProvider = { binding?.map },
            activityProvider = { activity },
            actions =
                object : NearbyViewportHelper.NearbyViewportActions {
                    override fun onPlaceHighlighted(place: Place) {
                        nearestPlace = place
                        highlightNearestPlace(place)
                    }

                    override fun onRecenterMapRequested(currentLatLng: LatLng?) {
                        recenterMap(currentLatLng)
                    }
                },
        )
    }

    private fun initFabHelper() {
        fabHelper.init(
            bindingProvider = { binding },
            contextProvider = { context },
            activityProvider = { activity },
            applicationKvStoreProvider = { applicationKvStore },
            actions =
                object : NearbyFabHelper.NearbyFabActions {
                    override fun isDepictsPickerMode(): Boolean = isDepictsPickerMode

                    override fun onPhotoMarkerCheckRequested() {
                        addPhotoMarkerIfDepictsMode()
                    }
                },
        )
    }

    private fun initFilterHelper() {
        filterHelper.init(
            bindingProvider = { binding },
            contextProvider = { context },
            activityProvider = { activity },
            dependencies =
                NearbyFilterHelper.NearbyFilterDependencies(
                    placeTypeLabelsClientProvider = { placeTypeLabelsClient },
                    compositeDisposableProvider = { compositeDisposable },
                    presenterProvider = { presenter },
                ),
            actions =
                object : NearbyFilterHelper.NearbyFilterActions {
                    override fun hideBottomSheet() {
                        bottomSheetHelper.hideBottomSheet()
                    }

                    override fun isDarkTheme(): Boolean = _isDarkTheme

                    override fun setHasOptionsMenu(hasMenu: Boolean) {
                        this@NearbyParentFragment.setHasOptionsMenu(hasMenu)
                    }
                },
        )
    }

    override fun setCheckBoxAction() {
        filterHelper.setCheckBoxAction()
    }

    override fun setCheckBoxState(state: Int) {
        filterHelper.setCheckBoxState(state)
    }

    override fun setFilterState() {
        filterHelper.setFilterState()
    }

    override fun centerMapToPlace(place: Place?) {
        viewportHelper.centerMapToPlace(place)
    }

    override fun updateListFragment(placeList: List<Place>) {
        bottomSheetHelper.updateListFragment(placeList)
    }

    override fun getLastLocation(): LatLng = locationHelper.getLastLocation()

    override fun getLastMapFocus(): LatLng = viewportHelper.getLastMapFocus()

    override fun getMapCenter(): LatLng = locationHelper.getMapCenter()

    override fun getMapFocus(): LatLng = viewportHelper.getMapFocus()

    override fun isAdvancedQueryFragmentVisible(): Boolean =
        filterHelper.isAdvancedQueryFragmentVisible()

    /**
     * Drops a custom Nearby query and loads places for the current map center again.
     * The advanced query editor stays open so the restored query text remains visible.
     */
    fun resetAdvancedQuery() {
        filterHelper.resetAdvancedQuery(presenter)
    }

    override fun showHideAdvancedQueryFragment(shouldShow: Boolean) {
        filterHelper.showHideAdvancedQueryFragment(shouldShow)
    }

    override fun isNetworkConnectionEstablished(): Boolean = isInternetConnectionEstablished(activity)

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private fun initNetworkBroadCastReceiver() {
        broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (activity != null) {
                        if (isInternetConnectionEstablished(activity)) {
                            if (isNetworkErrorOccurred) {
                                presenter!!.updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
                                isNetworkErrorOccurred = false
                            }

                            if (snackbar != null) {
                                snackbar!!.dismiss()
                                snackbar = null
                            }
                            hasShownOfflinePinsMessage = false
                        } else {
                            if (snackbar == null) {
                                snackbar =
                                    Snackbar.make(
                                        view!!,
                                        fr.free.nrw.commons.R.string.no_internet,
                                        Snackbar.LENGTH_INDEFINITE,
                                    )
                                hasShownOfflinePinsMessage = false
                                setProgressBarVisibility(false)
                            }

                            isNetworkErrorOccurred = true
                            snackbar!!.show()
                        }
                    }
                }
            }
    }

    /**
     * Updates the internet unavailable snackbar to reflect whether cached pins are shown.
     *
     * @param offlinePinsShown Whether there are pins currently being shown on map.
     */
    override fun updateSnackbar(offlinePinsShown: Boolean) {
        if (!isNetworkErrorOccurred || snackbar == null) {
            return
        }
        // Avoid flickering between offline messages while panning; switch once per offline session.
        if (offlinePinsShown && !hasShownOfflinePinsMessage) {
            snackbar!!.setText(fr.free.nrw.commons.R.string.nearby_showing_pins_offline)
            hasShownOfflinePinsMessage = true
        }
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    override fun listOptionMenuItemClicked() {
        bottomSheetHelper.listOptionMenuItemClicked()
    }

    override fun getScreenTopRight(): LatLng? = viewportHelper.getScreenTopRight()

    override fun getScreenBottomLeft(): LatLng? = viewportHelper.getScreenBottomLeft()

    override fun populatePlaces(currentLatLng: LatLng) {
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        var screenTopRightLatLng = screenBottomLeft
        var screenBottomLeftLatLng = screenTopRight

        // When the nearby fragment is opened immediately upon app launch, the {screenTopRightLatLng}
        // and {screenBottomLeftLatLng} variables return {LatLng(0.0,0.0)} as output.
        // To address this issue, A small delta value {delta = 0.02} is used to adjust the latitude
        // and longitude values for {ZOOM_LEVEL = 15f}.
        // This adjustment helps in calculating the east and west corner LatLng accurately.
        // Note: This only happens when the nearby fragment is opened immediately upon app launch,
        // otherwise {screenTopRightLatLng} and {screenBottomLeftLatLng} are used to determine
        // the east and west corner LatLng.
        if (screenTopRightLatLng?.latitude == 0.0 &&
            screenTopRightLatLng?.longitude == 0.0 &&
            screenBottomLeftLatLng?.latitude == 0.0 &&
            screenBottomLeftLatLng.longitude == 0.0
        ) {
            val delta = FALLBACK_BOUNDS_DELTA_DEGREES
            val westCornerLat = currentLatLng.latitude - delta
            val westCornerLong = currentLatLng.longitude - delta
            val eastCornerLat = currentLatLng.latitude + delta
            val eastCornerLong = currentLatLng.longitude + delta
            screenTopRightLatLng =
                LatLng(
                    westCornerLat,
                    westCornerLong,
                    0f,
                )
            screenBottomLeftLatLng =
                LatLng(
                    eastCornerLat,
                    eastCornerLong,
                    0f,
                )
            if (currentLatLng.equals(
                    getLastMapFocus(),
                )
            ) { // Means we are checking around current location
                populatePlacesForCurrentLocation(
                    mapFocus,
                    screenTopRightLatLng,
                    screenBottomLeftLatLng,
                    currentLatLng,
                    null,
                )
            } else {
                populatePlacesForAnotherLocation(
                    mapFocus,
                    screenTopRightLatLng,
                    screenBottomLeftLatLng,
                    currentLatLng,
                    null,
                )
            }
        } else {
            if (currentLatLng.equals(
                    getLastMapFocus(),
                )
            ) { // Means we are checking around current location
                populatePlacesForCurrentLocation(
                    mapFocus,
                    screenTopRightLatLng,
                    screenBottomLeftLatLng,
                    currentLatLng,
                    null,
                )
            } else {
                populatePlacesForAnotherLocation(
                    mapFocus,
                    screenTopRightLatLng,
                    screenBottomLeftLatLng,
                    currentLatLng,
                    null,
                )
            }
        }

        if (recenterToUserLocation) {
            recenterToUserLocation = false
        }
    }

    override fun populatePlaces(
        currentLatLng: LatLng,
        customQuery: String?,
    ) {
        if (customQuery == null || customQuery.isEmpty()) {
            populatePlaces(currentLatLng)
            return
        }
        // these two variables have historically been assigned values the opposite of what their
        // names imply, and quite some existing code depends on this fact
        val screenTopRightLatLng = screenBottomLeft
        val screenBottomLeftLatLng = screenTopRight

        if (currentLatLng.equals(lastFocusLocation) || lastFocusLocation == null || recenterToUserLocation) {
            // Means we are checking around current location
            populatePlacesForCurrentLocation(
                lastKnownLocation,
                screenTopRightLatLng,
                screenBottomLeftLatLng,
                currentLatLng,
                customQuery,
            )
        } else {
            populatePlacesForAnotherLocation(
                lastKnownLocation,
                screenTopRightLatLng,
                screenBottomLeftLatLng,
                currentLatLng,
                customQuery,
            )
        }
        if (recenterToUserLocation) {
            recenterToUserLocation = false
        }
    }

    /**
     * Clears the Nearby local cache and then calls for pin details to be fetched afresh.
     *
     */
    private fun emptyCache() {
        // reload the map once the cache is cleared
        compositeDisposable.add(
            placesRepository!!
                .clearCache()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(
                    Completable.fromAction {
                        // reload only the pin details, by making all loaded pins gray:
                        val newPlaceGroups =
                            ArrayList<MarkerPlaceGroup>(
                                NearbyController.markerLabelList.size,
                            )
                        for (placeGroup in NearbyController.markerLabelList) {
                            val place =
                                Place(
                                    "",
                                    "",
                                    placeGroup.place.label,
                                    "",
                                    placeGroup.place.getLocation(),
                                    "",
                                    placeGroup.place.siteLinks,
                                    "",
                                    placeGroup.place.exists,
                                    placeGroup.place.entityID,
                                )
                            place.setDistance(placeGroup.place.distance)
                            place.isMonument = placeGroup.place.isMonument
                            newPlaceGroups.add(
                                MarkerPlaceGroup(placeGroup.isBookmarked, place),
                            )
                        }
                        presenter!!.loadPlacesDataAsync(newPlaceGroups, scope)
                    },
                ).subscribe(
                    {
                        Timber.d("Nearby Cache cleared successfully.")
                    },
                    { throwable: Throwable? ->
                        Timber.e(throwable, "Failed to clear the Nearby Cache")
                    },
                ),
        )
    }

    override fun savePlacesAsKML() {
        fileExportHelper.savePlacesAsKML()
    }

    override fun savePlacesAsGPX() {
        fileExportHelper.savePlacesAsGPX()
    }

    /**
     * Adds a marker for the user's current position. Adds a circle which uses the accuracy * 2, to
     * draw a circle which represents the user's position with an accuracy of 95%.
     *
     *
     * Should be called only on creation of Map, there is other method to update markers location
     * with users move.
     *
     * @param currentLatLng current location
     */
    override fun addCurrentLocationMarker(currentLatLng: LatLng?) {
        markerOverlayHelper.addCurrentLocationMarker(currentLatLng)
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    override fun clearAllMarkers() {
        markerOverlayHelper.clearAllMarkers()
    }

    /**
     * Adds multiple markers representing places to the map and handles item gestures.
     * Markers are sorted by priority before being added so that higher-priority pins
     * (e.g. red "needs photo" pins) are always rendered on top of lower-priority ones
     * (e.g. green "has photo" pins) in the OSMDroid overlay list.
     *
     * @param markerPlaceGroups The list of marker place groups containing the places and
     * their bookmarked status
     */
    override fun replaceMarkerOverlays(markerPlaceGroups: List<MarkerPlaceGroup>) {
        markerOverlayHelper.replaceMarkerOverlays(markerPlaceGroups)
    }

    override fun filterOutAllMarkers() {
        markerOverlayHelper.filterOutAllMarkers()
    }

    /**
     * Filters markers based on selectedLabels and chips
     *
     * @param selectedLabels       label list that user clicked
     * @param filterForPlaceState  true if we filter places for place state
     * @param filterForAllNoneType true if we filter places with all none button
     */
    override fun filterMarkersByLabels(
        selectedLabels: List<Label>?,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean,
    ) {
        markerOverlayHelper.filterMarkersByLabels(
            selectedLabels,
            filterForPlaceState,
            filterForAllNoneType,
        )
    }

    /**
     * Fetches and updates the data for a specific place, then updates the corresponding marker on the map.
     *
     * @param place        The Place object containing the initial place data.
     * @param marker       The Marker object on the map representing the place.
     * @param isBookMarked A boolean indicating if the place is bookmarked.
     */
    private fun getPlaceData(
        place: Place,
        marker: Marker,
        isBookMarked: Boolean,
    ) {
        val getPlaceObservable =
            Observable
                .fromCallable {
                    nearbyController?.getPlaces(java.util.List.of(place))
                }
        compositeDisposable.add(
            getPlaceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { placeList: List<Place> ->
                        val updatedPlace = placeList[0]
                        updatedPlace.distance = place.distance
                        updatedPlace.location = place.location
                        marker.title = updatedPlace.name
                        marker.snippet =
                            if (containsParentheses(updatedPlace.longDescription)) {
                                getTextBetweenParentheses(
                                    updatedPlace.longDescription,
                                )
                            } else {
                                updatedPlace.longDescription
                            }
                        marker.showInfoWindow()
                        presenter!!.handlePinClicked(updatedPlace)
                        savePlaceToDatabase(updatedPlace)
                        val icon =
                            getCachedDrawable(
                                requireContext(),
                                fr.free.nrw.commons.nearby.fragments.getIconFor(
                                    updatedPlace,
                                    isBookMarked,
                                    nearestPlace,
                                ),
                                markerOverlayHelper.drawableCache,
                            )
                        marker.icon = icon
                        binding!!.map.invalidate()
                        binding!!.bottomSheetDetails.dataCircularProgress.visibility = View.GONE
                        binding!!.bottomSheetDetails.icon.visibility = View.VISIBLE
                        binding!!.bottomSheetDetails.wikiDataLl.visibility = View.VISIBLE
                        bottomSheetHelper.passInfoToSheet(updatedPlace)
                        bottomSheetHelper.hideBottomSheet()
                    },
                    { throwable: Throwable ->
                        Timber.d(throwable)
                        showErrorMessage(
                            getString(fr.free.nrw.commons.R.string.could_not_load_place_data) +
                                throwable.localizedMessage,
                        )
                    },
                ),
        )
    }

    private fun populatePlacesForCurrentLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng?,
        screenBottomLeft: LatLng?,
        searchLatLng: LatLng,
        customQuery: String?,
    ) {
        val nearbyPlacesInfoObservable =
            Observable
                .fromCallable {
                    nearbyController?.loadAttractionsFromLocation(
                        currentLatLng,
                        screenTopRight,
                        screenBottomLeft,
                        searchLatLng,
                        false,
                        true,
                        isMonumentsEnabled,
                        customQuery,
                    )
                }

        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo ->
                        if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                            showErrorMessage(getString(fr.free.nrw.commons.R.string.no_nearby_places_around))
                            setProgressBarVisibility(false)
                            presenter!!.lockUnlockNearby(false)
                        } else {
                            updateMapMarkers(nearbyPlacesInfo.placeList, searchLatLng)
                            lastFocusLocation = searchLatLng
                            lastMapFocus =
                                GeoPoint(
                                    searchLatLng.latitude,
                                    searchLatLng.longitude,
                                )
                        }
                    } as ((NearbyPlacesInfo?) -> Unit)?,
                    { throwable: Throwable ->
                        Timber.d(throwable)
                        showErrorMessage(
                            getString(fr.free.nrw.commons.R.string.error_fetching_nearby_places) +
                                throwable.localizedMessage,
                        )
                        setProgressBarVisibility(false)
                        presenter!!.lockUnlockNearby(false)
                        setFilterState()
                    },
                ),
        )
    }

    private fun populatePlacesForAnotherLocation(
        currentLatLng: LatLng?,
        screenTopRight: LatLng?,
        screenBottomLeft: LatLng?,
        searchLatLng: LatLng,
        customQuery: String?,
    ) {
        val nearbyPlacesInfoObservable =
            Observable
                .fromCallable {
                    nearbyController?.loadAttractionsFromLocation(
                        currentLatLng,
                        screenTopRight,
                        screenBottomLeft,
                        searchLatLng,
                        false,
                        true,
                        isMonumentsEnabled,
                        customQuery,
                    )
                }

        compositeDisposable.add(
            nearbyPlacesInfoObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { nearbyPlacesInfo: NearbyPlacesInfo ->
                        if (nearbyPlacesInfo.placeList == null || nearbyPlacesInfo.placeList.isEmpty()) {
                            showErrorMessage(getString(fr.free.nrw.commons.R.string.no_nearby_places_around))
                            setProgressBarVisibility(false)
                            presenter!!.lockUnlockNearby(false)
                        } else {
                            // Updating last searched location
                            applicationKvStore!!.putString(
                                "LastLocation",
                                searchLatLng.latitude.toString() + "," + searchLatLng.longitude,
                            )

                            // curLatLng is used to calculate distance from the current location to the place
                            // and distance is later on populated to the place
                            updateMapMarkers(nearbyPlacesInfo.placeList, searchLatLng)
                            lastMapFocus =
                                GeoPoint(
                                    searchLatLng.latitude,
                                    searchLatLng.longitude,
                                )
                            stopQuery()
                        }
                    } as ((NearbyPlacesInfo?) -> Unit)?,
                    { throwable: Throwable ->
                        Timber.e(throwable)
                        showErrorMessage(
                            getString(fr.free.nrw.commons.R.string.error_fetching_nearby_places) +
                                throwable.localizedMessage,
                        )
                        setProgressBarVisibility(false)
                        presenter!!.lockUnlockNearby(false)
                        setFilterState()
                    },
                ),
        )
    }

    fun savePlaceToDatabase(place: Place?) {
        placesRepository?.save(place)?.subscribeOn(Schedulers.io())?.subscribe()?.let {
            compositeDisposable.add(
                it,
            )
        }
    }

    /**
     * Stops any ongoing queries and clears all disposables.
     * This method sets the stopQuery flag to true and clears the compositeDisposable
     * to prevent any further processing.
     */
    override fun stopQuery() {
        stopQuery = true
        compositeDisposable.clear()
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     *
     * @param nearbyPlaces This variable has place list information and distances.
     */
    private fun updateMapMarkers(
        nearbyPlaces: List<Place>,
        curLatLng: LatLng,
    ) {
        presenter!!.updateMapMarkers(nearbyPlaces, curLatLng, scope)
    }

    override fun isListBottomSheetExpanded(): Boolean =
        bottomSheetHelper.isListBottomSheetExpanded()

    override fun isDetailsBottomSheetVisible(): Boolean =
        bottomSheetHelper.isDetailsBottomSheetVisible()

    override fun isNearbyFilterListVisible(): Boolean = filterHelper.isNearbyFilterListVisible()

    override fun hideNearbyFilterList() {
        filterHelper.hideNearbyFilterList()
    }

    override fun setBottomSheetDetailsSmaller() {
        bottomSheetHelper.setBottomSheetDetailsSmaller()
    }

    override fun setRecyclerViewAdapterAllSelected() {
        filterHelper.setRecyclerViewAdapterAllSelected()
    }

    override fun setRecyclerViewAdapterItemsGreyedOut() {
        filterHelper.setRecyclerViewAdapterItemsGreyedOut()
    }

    override fun setProgressBarVisibility(isVisible: Boolean) {
        if (isVisible) {
            binding!!.mapProgressBar.visibility = View.VISIBLE
        } else {
            binding!!.mapProgressBar.visibility = View.GONE
        }
    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    override fun animateFABs() {
        fabHelper.animateFABs()
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are
     * cleared.
     */
    override fun displayLoginSkippedWarning() {
        fabHelper.displayLoginSkippedWarning()
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) {
        locationHelper.locationUpdateListener.onLocationChangedSignificantly(latLng)
    }

    override fun onLocationChangedSlightly(latLng: LatLng) {
        locationHelper.locationUpdateListener.onLocationChangedSlightly(latLng)
    }

    override fun onLocationChangedMedium(latLng: LatLng) {
        locationHelper.locationUpdateListener.onLocationChangedMedium(latLng)
    }

    fun backButtonClicked(): Boolean {
        if (::mediaDetails.isInitialized && mediaDetails.isVisible) {
            removeFragment(mediaDetails)

            binding?.coordinatorLayout?.visibility = View.VISIBLE
            binding?.map?.setMultiTouchControls(true)
            binding?.map?.isClickable = true

            val transaction = childFragmentManager.beginTransaction()
            val fragmentContainer = childFragmentManager.findFragmentById(R.id.coordinator_layout)

            if (fragmentContainer != null) {
                transaction.show(fragmentContainer)
            }

            transaction.commit()
            childFragmentManager.executePendingTransactions()

            (activity as? MainActivity)?.showTabs()
            (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            return true
        } else {
            (activity as? MainActivity)?.setSelectedItemId(NavTab.NEARBY.code())
        }

        return presenter?.backButtonClicked() ?: false
    }

    override fun onLocationPermissionDenied(toastMessage: String) {
        locationHelper.locationPermissionCallback.onLocationPermissionDenied(toastMessage)
    }

    override fun onLocationPermissionGranted() {
        locationHelper.locationPermissionCallback.onLocationPermissionGranted()
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are
     * cleared.
     */
    override fun setFABPlusAction(onClickListener: View.OnClickListener) {
        fabHelper.setFABPlusAction(onClickListener)
    }

    override fun setFABRecenterAction(onClickListener: View.OnClickListener) {
        fabHelper.setFABRecenterAction(onClickListener)
    }

    /**
     * The [NearbyParentFragmentPresenter.lockUnlockNearby] disables enables FAB recenter.
     * This lets us [addPhotoMarkerIfDepictsMode] pre-populating before place markers
     * if nearby places found are null and place markers are not rendered
     */
    override fun disableFABRecenter() {
        fabHelper.disableFABRecenter()
    }

    override fun enableFABRecenter() {
        fabHelper.enableFABRecenter()
    }

    override fun getCameraTarget(): LatLng? = viewportHelper.getCameraTarget()

    /**
     * Highlights nearest place when user clicks on home nearby banner
     *
     * @param nearestPlace nearest place, which has to be highlighted
     */
    private fun highlightNearestPlace(nearestPlace: Place) {
        binding!!.bottomSheetDetails.icon.visibility = View.VISIBLE
        bottomSheetHelper.passInfoToSheet(nearestPlace)
        bottomSheetHelper.hideBottomSheet()
        bottomSheetHelper.bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    /**
     * Returns a rendering priority for a marker derived from [getIconFor], where a higher value
     * means the marker is drawn on top (OSMDroid renders overlays last-in = on top).
     * All place-type logic stays in [getIconFor]; this method only maps icon → priority.
     *
     *  5 – Purple  : nearest / highlighted place
     *  4 – Red     : exists, needs a photo  ← must always be above green
     *  3 – Monument: WLM monument markers
     *  2 – Green   : already has a photo
     *  1 – Grey    : loading (no name yet)
     *  0 – Clear   : does not exist in the real world
     */
    private fun getMarkerPriority(
        place: Place,
        isBookmarked: Boolean,
    ): Int = fr.free.nrw.commons.nearby.fragments.getMarkerPriority(place, isBookmarked, nearestPlace)

    override fun recenterMap(currentLatLng: LatLng?) {
        locationHelper.recenterMap(currentLatLng)
    }

    override fun openLocationSettings() {
        locationHelper.openLocationSettings()
    }

    override fun hideBottomSheet() {
        bottomSheetHelper.hideBottomSheet()
    }

    override fun hideBottomDetailsSheet() {
        bottomSheetHelper.hideBottomDetailsSheet()
    }

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus If nearby details bottom
     * sheet state is expanded: show fab plus If nearby details bottom sheet state is hidden: hide
     * all fabs
     *
     * @param bottomSheetState see bottom sheet states
     */
    fun prepareViewsForSheetPosition(bottomSheetState: Int) {
        fabHelper.prepareViewsForSheetPosition(bottomSheetState)
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private fun passInfoToSheet(place: Place) {
        bottomSheetHelper.passInfoToSheet(place)
    }

    private fun handleMediaClick(imageName: String) {
        val decodedImageName = URLDecoder.decode(imageName, StandardCharsets.UTF_8.toString())

        mediaClient
            .getMedia("File:$decodedImageName")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mediaResponse ->
                if (mediaResponse != null) {
                    // Create a Media object from the response
                    media =
                        Media(
                            pageId = mediaResponse.pageId ?: UUID.randomUUID().toString(),
                            thumbUrl = mediaResponse.thumbUrl,
                            imageUrl = mediaResponse.imageUrl,
                            filename = mediaResponse.filename,
                            fallbackDescription = mediaResponse.fallbackDescription,
                            dateUploaded = mediaResponse.dateUploaded,
                            license = mediaResponse.license,
                            licenseUrl = mediaResponse.licenseUrl,
                            author = mediaResponse.author,
                            user = mediaResponse.user,
                            categories = mediaResponse.categories,
                            coordinates = mediaResponse.coordinates,
                            captions = mediaResponse.captions ?: emptyMap(),
                            descriptions = mediaResponse.descriptions ?: emptyMap(),
                            depictionIds = mediaResponse.depictionIds ?: emptyList(),
                            categoriesHiddenStatus = mediaResponse.categoriesHiddenStatus ?: emptyMap(),
                            mimeType = mediaResponse.mimeType,
                            mediaType = mediaResponse.mediaType,
                        )
                    // Remove existing fragment before showing new details
                    if (::mediaDetails.isInitialized && mediaDetails.isAdded) {
                        removeFragment(mediaDetails)
                    }
                    showMediaDetails()
                } else {
                    Timber.e("Fetched media is null for image: $decodedImageName")
                }
            }, { throwable ->
                Timber.e(throwable, "Error fetching media for image: $decodedImageName")
            })
    }

    private fun showMediaDetails() {
        binding?.map?.setMultiTouchControls(false)
        binding?.map?.isClickable = false

        mediaDetails = MediaDetailPagerFragment.newInstance(false, true)

        val transaction = childFragmentManager.beginTransaction()

        val fragmentContainer = childFragmentManager.findFragmentById(R.id.coordinator_layout)
        if (fragmentContainer != null) {
            transaction.hide(fragmentContainer)
        }

        // Replace instead of add to ensure new fragment is used
        transaction.replace(R.id.coordinator_layout, mediaDetails, "MediaDetailFragmentTag")
        transaction.addToBackStack("Nearby_Parent_Fragment_Tag").commit()
        childFragmentManager.executePendingTransactions()

        (activity as? MainActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (mediaDetails.isAdded) {
            mediaDetails.showImage(0)
        } else {
            Timber.e("Error: MediaDetailPagerFragment is NOT added")
        }
    }

    override fun getMediaAtPosition(i: Int): Media? = media

    override fun getTotalMediaCount(): Int = 2

    override fun getContributionStateAt(position: Int): Int? = null

    override fun refreshNominatedMedia(index: Int) {
        if (this::mediaDetails.isInitialized && !binding?.map?.isClickable!! == true) {
            removeFragment(mediaDetails)
            showMediaDetails()
        }
    }

    private fun removeFragment(fragment: Fragment) {
        childFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commit()
        childFragmentManager.executePendingTransactions()
    }

    private fun storeSharedPrefs(selectedPlace: Place) {
        applicationKvStore!!.putJson<Place>(WikidataConstants.PLACE_OBJECT, selectedPlace)
        val place =
            applicationKvStore!!.getJson<Place>(WikidataConstants.PLACE_OBJECT, Place::class.java)

        Timber.d("Stored place object %s", place.toString())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        wikidataEditListener!!.authenticationStateListener = this
    }

    override fun onDestroy() {
        super.onDestroy()
        wikidataEditListener!!.authenticationStateListener = null
    }

    override fun onWikidataEditSuccessful() {
        if (presenter != null && locationManager != null) {
            presenter!!.updateMapAndList(LocationChangeType.MAP_UPDATED)
        }
    }

    private fun showErrorMessage(message: String) {
        Timber.e(message)
        showLongToast(requireActivity(), message)
    }

    fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
        locationHelper.registerUnregisterLocationListener(removeLocationListener)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        this.isVisibleToUser = isVisibleToUser
        if (isResumed && isVisibleToUser) {
            performMapReadyActions()
        } else {
            bottomSheetHelper.hideBottomSheet()
            bottomSheetHelper.hideBottomDetailsSheet()
        }
    }

    /**
     * Updates the user current location overlays (both the location and error overlays) by
     * replacing any existing location overlays with new overlays at the given GeoPoint. If there
     * are no existing location and error overlays, then new overlays are added.
     *
     * @param geoPoint The GeoPoint representing the user's current location.
     * @param invalidate If true, the map overlays will be invalidated after the user
     * location overlays are updated/added. If false, the overlays will not be invalidated.
     */
    private fun updateUserLocationOverlays(
        geoPoint: GeoPoint?,
        invalidate: Boolean,
    ) {
        markerOverlayHelper.updateUserLocationOverlays(geoPoint, invalidate)
    }

    private fun moveCameraToPosition(geoPoint: GeoPoint) {
        viewportHelper.moveCameraToPosition(geoPoint)
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint at specified zoom level and speed
     * using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     * @param zoom     Zoom level of the map camera
     * @param speed    Speed of animation
     */
    private fun moveCameraToPosition(
        geoPoint: GeoPoint,
        zoom: Double,
        speed: Long,
    ) {
        viewportHelper.moveCameraToPosition(geoPoint, zoom, speed)
    }

    override fun onBottomSheetItemClick(
        view: View?,
        position: Int,
    ) {
        bottomSheetHelper.itemClickListener.onBottomSheetItemClick(view, position)
    }

    override fun onBottomSheetItemLongClick(
        view: View?,
        position: Int,
    ) {
        bottomSheetHelper.itemClickListener.onBottomSheetItemLongClick(view, position)
    }

    interface NearbyParentFragmentInstanceReadyCallback {
        fun onReady()
    }

    fun setNearbyParentFragmentInstanceReadyCallback(
        nearbyParentFragmentInstanceReadyCallback: NearbyParentFragmentInstanceReadyCallback?,
    ) {
        this.nearbyParentFragmentInstanceReadyCallback = nearbyParentFragmentInstanceReadyCallback
    }

    private fun applyNearbyFabLayout() {
        fabHelper.applyNearbyFabLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rlBottomSheetLayoutParams = binding!!.bottomSheetNearby.bottomSheet.layoutParams
        rlBottomSheetLayoutParams.height =
            requireActivity().windowManager.defaultDisplay.height /
                BOTTOM_SHEET_HEIGHT_DIVISOR * BOTTOM_SHEET_HEIGHT_MULTIPLIER
        binding!!.bottomSheetNearby.bottomSheet.layoutParams = rlBottomSheetLayoutParams
        val orientation = resources.configuration.orientation
        val adapterCount = bottomSheetHelper.bottomSheetAdapter?.itemCount ?: 0
        val spanCount =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                PORTRAIT_SPAN_COUNT
            } else {
                if (adapterCount > 0) adapterCount else LANDSCAPE_SPAN_COUNT
            }
        (binding?.bottomSheetDetails?.bottomSheetRecyclerView?.layoutManager as? GridLayoutManager)?.spanCount =
            spanCount
        applyNearbyFabLayout()
    }

    fun onLearnMoreClicked() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(wlmUrl))
        startActivity(intent)
    }

    companion object {
        private const val ZOOM_LEVEL = 15f
        private const val DEFAULT_LATITUDE = 51.50550
        private const val DEFAULT_LONGITUDE = -0.07520
        private const val SCALE_BAR_OFFSET_X = 15
        private const val SCALE_BAR_OFFSET_Y = 25
        private const val SCALE_BAR_BACKGROUND_ALPHA = 200
        private const val SCALE_BAR_BACKGROUND_RED = 255
        private const val SCALE_BAR_BACKGROUND_GREEN = 250
        private const val SCALE_BAR_BACKGROUND_BLUE = 250
        private const val BOUNDING_BOX_BORDER_SIZE_PX = 100
        private const val PORTRAIT_SPAN_COUNT = 3
        private const val LANDSCAPE_SPAN_COUNT = 6
        private const val BOTTOM_SHEET_HEIGHT_DIVISOR = 16
        private const val BOTTOM_SHEET_HEIGHT_MULTIPLIER = 9
        private const val FALLBACK_BOUNDS_DELTA_DEGREES = 0.009
        const val ARG_PICKER_MODE = "is_picker_mode"
        const val ARG_PHOTO_LATLNGS = "picker_photo_latlngs"
        const val PICKER_RESULT_PLACE = "map_picked_place"

        /**
         * WLM URL
         */
        const val WLM_URL: String =
            "https://commons.wikimedia.org/wiki/Commons:Mobile_app/Contributing_to_WLM_using_the_app"

        @JvmStatic // This makes it callable as a static method from Java
        fun newInstance(): NearbyParentFragment {
            val fragment = NearbyParentFragment()
            fragment.retainInstance = true
            return fragment
        }

        @JvmStatic
        fun newPickerInstance(photoLatLngs: ArrayList<LatLng>?): NearbyParentFragment {
            val fragment = NearbyParentFragment()
            val args = Bundle()
            args.putBoolean(ARG_PICKER_MODE, true)
            args.putParcelableArrayList(ARG_PHOTO_LATLNGS, photoLatLngs)
            fragment.arguments = args
            return fragment
        }

        /**
         * Extracts text between the first occurrence of '(' and its corresponding ')' in the input
         * string.
         *
         * @param input The input string from which to extract text between parentheses.
         * @return The text between parentheses if found, or `null` if no parentheses are found.
         */
        fun getTextBetweenParentheses(input: String): String? {
            val startIndex = input.indexOf('(')
            val endIndex = input.indexOf(')', startIndex)
            return if (startIndex != -1 && endIndex != -1) {
                input.substring(startIndex + 1, endIndex)
            } else {
                null
            }
        }

        /**
         * Checks if the given text contains '(' or ')'.
         *
         * @param input The input text to check.
         * @return `true` if '(' or ')' is found, `false` otherwise.
         */
        fun containsParentheses(input: String): Boolean = input.contains("(") || input.contains(")")
    }
}

