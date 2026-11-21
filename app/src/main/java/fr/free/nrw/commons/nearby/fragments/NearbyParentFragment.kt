package fr.free.nrw.commons.nearby.fragments

import android.Manifest.permission
import android.app.ProgressDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaDetailProvider
import fr.free.nrw.commons.nearby.BottomSheetAdapter
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlaceTypeLabelsClient
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.SystemThemeUtils
import fr.free.nrw.commons.wikidata.WikidataEditListener
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import io.reactivex.disposables.CompositeDisposable
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment displaying Nearby places on an interactive OSMDroid map with search filters,
 * place markers, location tracking, camera viewports, and place detail sheets.
 */
class NearbyParentFragment(
    val helpers: NearbyHelpers = NearbyHelpers(),
) : CommonsDaggerSupportFragment(),
    NearbyParentFragmentContract.View,
    NearbyParentFragmentContract.FileExport by helpers.fileExportHelper,
    NearbyParentFragmentContract.MarkerOverlay by helpers.markerOverlayHelper,
    NearbyParentFragmentContract.BottomSheet by helpers.bottomSheetHelper,
    NearbyParentFragmentContract.Location by helpers.locationHelper,
    NearbyParentFragmentContract.Viewport by helpers.viewportHelper,
    NearbyParentFragmentContract.Filter by helpers.filterHelper,
    NearbyParentFragmentContract.FabControl by helpers.fabHelper,
    NearbyParentFragmentContract.PlacesQuery by helpers.placesQueryHelper,
    NearbyParentFragmentContract.StatusControl by helpers.statusHelper,
    LocationUpdateListener by helpers.locationHelper.locationUpdateListener,
    LocationPermissionCallback by helpers.locationHelper.locationPermissionCallback,
    BottomSheetAdapter.ItemClickListener by helpers.bottomSheetHelper.itemClickListener,
    MediaDetailProvider by helpers.mediaDetailHelper,
    WikidataP18EditListener by helpers.statusHelper {

    constructor() : this(NearbyHelpers())

    val fileExportHelper: NearbyFileExportHelper get() = helpers.fileExportHelper
    val markerOverlayHelper: NearbyMarkerOverlayHelper get() = helpers.markerOverlayHelper
    val bottomSheetHelper: NearbyBottomSheetHelper get() = helpers.bottomSheetHelper
    val locationHelper: NearbyLocationHelper get() = helpers.locationHelper
    val viewportHelper: NearbyViewportHelper get() = helpers.viewportHelper

    val filterHelper: NearbyFilterHelper get() = helpers.filterHelper
    val fabHelper: NearbyFabHelper get() = helpers.fabHelper

    val mediaDetailHelper: NearbyMediaDetailHelper get() = helpers.mediaDetailHelper
    val placesQueryHelper: NearbyPlacesQueryHelper get() = helpers.placesQueryHelper
    val statusHelper: NearbyStatusHelper get() = helpers.statusHelper

    val disposables: CompositeDisposable get() = compositeDisposable

    var binding: FragmentNearbyParentBinding? = null

    internal val mapEventsOverlay: MapEventsOverlay =
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    if (markerOverlayHelper.clickedMarker != null) {
                        markerOverlayHelper.clickedMarker!!.closeInfoWindow()
                    } else {
                        Timber.e("CLICKED MARKER IS NULL")
                    }
                    if (isListBottomSheetExpanded) {
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
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    @Inject
    lateinit var mediaClient: MediaClient

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

    internal var locationPermissionsHelper: LocationPermissionsHelper? = null

    internal var scope: LifecycleCoroutineScope? = null

    @VisibleForTesting
    var presenter: NearbyParentFragmentPresenter? = null

    internal var isDarkTheme = false

    internal var progressDialog: ProgressDialog? = null

    private var isVisibleToUser = false

    internal var adapter: PlaceAdapter? = null

    @JvmField
    internal var nearbyParentFragmentInstanceReadyCallback: NearbyParentFragmentInstanceReadyCallback? = null

    internal var isDepictsPickerMode: Boolean = false

    internal val galleryPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        // Handle the result from the gallery
                        controller?.onPictureReturnedFromGallery(result, requireActivity(), callbacks)
                    }
                },
            )
        }

    internal val customSelectorLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        if (result != null) {
                            controller?.onPictureReturnedFromCustomSelector(result, requireActivity(), callbacks)
                        }
                    }
                },
            )
        }

    internal val cameraPickLauncherForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            controller?.handleActivityResultWithCallback(
                requireActivity(),
                object : FilePicker.HandleActivityResult {
                    override fun onHandleActivityResult(callbacks: FilePicker.Callbacks) {
                        if (result != null) {
                            controller?.onPictureReturnedFromCamera(result, requireActivity(), callbacks)
                        }
                    }
                },
            )
        }

    internal lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

    internal val locationPermissionLauncher: ActivityResultLauncher<String> =
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
                        { askForLocationPermission() },
                        null,
                        null,
                    )
                } else {
                    if (locationHelper.isPermissionDenied) {
                        locationPermissionsHelper?.showAppSettingsDialog(
                            requireActivity(),
                            fr.free.nrw.commons.R.string.nearby_needs_location,
                        )
                    }
                    Timber.d("The user checked 'Don't ask again' or denied the permission twice")
                    locationHelper.markPermissionDenied()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        Label.init(requireContext().applicationContext)
        viewportHelper.loadExploreMapData(arguments)

        binding = FragmentNearbyParentBinding.inflate(inflater, container, false)

        scope = viewLifecycleOwner.lifecycleScope
        presenter =
            NearbyParentFragmentPresenter(
                bookmarkLocationDao,
                placesRepository,
                nearbyController,
            )
        progressDialog = ProgressDialog(activity)
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage("Saving in progress...")
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return binding?.root
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
            runNearbyMenuAction { emptyCache() }
            false
        }
        listMenu.setOnMenuItemClickListener {
            listOptionMenuItemClicked()
            false
        }

        showInExploreButton.setOnMenuItemClickListener {
            (context as MainActivity).loadExploreMapFromNearby(
                binding?.map?.zoomLevelDouble ?: 0.0,
                binding?.map?.mapCenter?.latitude ?: 0.0,
                binding?.map?.mapCenter?.longitude ?: 0.0,
            )
            true
        }

        saveAsGPXButton.setOnMenuItemClickListener {
            runNearbyMenuAction {
                progressDialog!!.setTitle(getString(fr.free.nrw.commons.R.string.saving_gpx_file))
                progressDialog!!.show()
                savePlacesAsGPX()
            }
            false
        }
        saveAsKMLButton.setOnMenuItemClickListener {
            runNearbyMenuAction {
                progressDialog!!.setTitle(getString(fr.free.nrw.commons.R.string.saving_kml_file))
                progressDialog!!.show()
                savePlacesAsKML()
            }
            false
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initializeHelpersAndViews()
    }

    override fun onResume() {
        super.onResume()
        binding?.map?.onResume()
        markerOverlayHelper.clickedMarker?.infoWindow?.isOpen?.let {
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

    override fun onPause() {
        super.onPause()
        binding?.map?.onPause()
        compositeDisposable.clear()
        presenter?.detachView()
        registerUnregisterLocationListener(true)
        try {
            val receiver = statusHelper.broadcastReceiver
            if (receiver != null && activity != null) {
                requireContext().unregisterReceiver(receiver)
            }

            if (locationManager != null && presenter != null) {
                locationManager.removeLocationListener(presenter!!)
                locationManager.unregisterLocationManager()
            }
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
            // Broadcast receivers should always be unregistered inside catch,
            // you never know if they were already registered or not
        } catch (e: IllegalStateException) {
            Timber.e(e)
        } catch (e: SecurityException) {
            Timber.e(e)
        }
        statusHelper.dismissSnackbar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
        filterHelper.onDestroyView()

        if (presenter == null) Timber.w("NearbyParentFragment: presenter is null")
        if (applicationKvStore == null) Timber.w("NearbyParentFragment: applicationKvStore is null")

        if (!::controller.isInitialized || !controller.isInternalUploadInProgress) {
            presenter?.removeNearbyPreferences(applicationKvStore ?: return)
        }
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        wikidataEditListener.authenticationStateListener = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val currentBinding = binding ?: return
        val rlBottomSheetLayoutParams = currentBinding.bottomSheetNearby.bottomSheet.layoutParams
        rlBottomSheetLayoutParams.height =
            requireActivity().windowManager.defaultDisplay.height /
                BOTTOM_SHEET_HEIGHT_DIVISOR * BOTTOM_SHEET_HEIGHT_MULTIPLIER
        currentBinding.bottomSheetNearby.bottomSheet.layoutParams = rlBottomSheetLayoutParams
        val orientation = resources.configuration.orientation
        val adapterCount = bottomSheetHelper.bottomSheetAdapter?.itemCount ?: 0
        val spanCount =
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                PORTRAIT_SPAN_COUNT
            } else {
                if (adapterCount > 0) adapterCount else LANDSCAPE_SPAN_COUNT
            }
        (currentBinding.bottomSheetDetails.bottomSheetRecyclerView.layoutManager as? GridLayoutManager)?.spanCount =
            spanCount
        applyNearbyFabLayout()
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
     * Returns a rendering priority for a marker derived from [getIconFor], where a higher value
     * means the marker is drawn on top (OSMDroid renders overlays last-in = on top).
     * Retained for test reflection compatibility in testMarkerPriorityOrder.
     */
    private fun getMarkerPriority(
        place: Place,
        isBookmarked: Boolean,
    ): Int = fr.free.nrw.commons.nearby.fragments.getMarkerPriority(place, isBookmarked, viewportHelper.nearestPlace)

    interface NearbyParentFragmentInstanceReadyCallback {
        fun onReady()
    }

    companion object {
        const val ZOOM_LEVEL = 15f
        const val DEFAULT_LATITUDE = 51.50550
        const val DEFAULT_LONGITUDE = -0.07520
        const val SCALE_BAR_OFFSET_X = 15
        const val SCALE_BAR_OFFSET_Y = 25
        const val SCALE_BAR_BACKGROUND_ALPHA = 200
        const val SCALE_BAR_BACKGROUND_RED = 255
        const val SCALE_BAR_BACKGROUND_GREEN = 250
        const val SCALE_BAR_BACKGROUND_BLUE = 250
        const val BOUNDING_BOX_BORDER_SIZE_PX = 100
        const val PORTRAIT_SPAN_COUNT = 3
        const val LANDSCAPE_SPAN_COUNT = 6
        const val BOTTOM_SHEET_HEIGHT_DIVISOR = 16
        const val BOTTOM_SHEET_HEIGHT_MULTIPLIER = 9
        const val ARG_PICKER_MODE = "is_picker_mode"
        const val ARG_PHOTO_LATLNGS = "picker_photo_latlngs"
        const val PICKER_RESULT_PLACE = "map_picked_place"

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

// Cache and export actions surface all storage failures as IllegalStateException to the menu host.
@Suppress("TooGenericExceptionCaught")
private inline fun runNearbyMenuAction(action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        throw IllegalStateException(e)
    }
}
