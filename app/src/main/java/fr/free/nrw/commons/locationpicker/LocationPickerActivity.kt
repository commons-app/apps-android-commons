package fr.free.nrw.commons.locationpicker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.CsrfTokenClient
import fr.free.nrw.commons.coordinates.CoordinateEditHelper
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.Companion.LAST_LOCATION
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.Companion.LAST_ZOOM
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomInsets
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomPaddingInsets
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopPaddingInsets
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

/**
 * Helps to pick location and return the result with an intent
 */
class LocationPickerActivity : BaseActivity() {
    /**
     * coordinateEditHelper: helps to edit coordinates
     */
    @Inject
    lateinit var coordinateEditHelper: CoordinateEditHelper

    /**
     * media : Media object
     */
    private var media: Media? = null

    /**
     * cameraPosition : position of picker
     */
    private var cameraPosition: CameraPosition? = null

    /**
     * markerImage : picker image
     */
    private lateinit var markerImage: ImageView

    /**
     * mapView : OSM Map
     */
    private var mapView: org.osmdroid.views.MapView? = null
    private lateinit var mapManager: LocationPickerMapManager
    private lateinit var permissionManager: LocationPickerPermissionManager

    /**
     * tvAttribution : credit
     */
    private lateinit var tvAttribution: AppCompatTextView

    /**
     * activity : activity key
     */
    private var activity: String? = null

    /**
     * modifyLocationButton : button for start editing location
     */
    private lateinit var modifyLocationButton: Button

    /**
     * removeLocationButton : button to remove location metadata
     */
    private lateinit var removeLocationButton: Button

    /**
     * showInMapButton : button for showing in map
     */
    private lateinit var showInMapButton: TextView

    /**
     * placeSelectedButton : fab for selecting location
     */
    private lateinit var placeSelectedButton: FloatingActionButton

    /**
     * fabCenterOnLocation: button for center on location;
     */
    private lateinit var fabCenterOnLocation: FloatingActionButton

    /**
     * shadow : imageview of shadow
     */
    private lateinit var shadow: ImageView

    /**
     * largeToolbarText : textView of shadow
     */
    private lateinit var largeToolbarText: TextView

    /**
     * smallToolbarText : textView of shadow
     */
    private lateinit var smallToolbarText: TextView

    /**
     * applicationKvStore : for storing values
     */
    @Inject
    @field:Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore
    private lateinit var store: BasicKvStore

    /**
     * isDarkTheme: for keeping a track of the device theme and modifying the map theme accordingly
     */
    private var isDarkTheme: Boolean = false
    private var moveToCurrentLocation: Boolean = false

    @Inject
    lateinit var locationManager: LocationServiceManager

    @Inject
    lateinit var sessionManager: SessionManager

    /**
     * Constants
     */
    companion object {
        private const val CAMERA_POS = "cameraPosition"
        private const val ACTIVITY = "activity"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR)
        super.onCreate(savedInstanceState)

        isDarkTheme = systemThemeUtils.isDeviceInNightMode()
        moveToCurrentLocation = false
        store = BasicKvStore(this, "LocationPermissions")

        requestWindowFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_location_picker)

        if (savedInstanceState == null) {
            cameraPosition =
                IntentCompat.getParcelableExtra(
                    intent,
                    LocationPickerConstants.MAP_CAMERA_POSITION,
                    CameraPosition::class.java,
                )
            activity = intent.getStringExtra(LocationPickerConstants.ACTIVITY_KEY)
            media =
                IntentCompat.getParcelableExtra(
                    intent,
                    LocationPickerConstants.MEDIA,
                    Media::class.java,
                )
        } else {
            cameraPosition =
                BundleCompat.getParcelable(
                    savedInstanceState,
                    CAMERA_POS,
                    CameraPosition::class.java,
                )
            activity = savedInstanceState.getString(ACTIVITY)
            media = BundleCompat.getParcelable(savedInstanceState, "sMedia", Media::class.java)
        }

        bindViewsAndListeners()

        org.osmdroid.config.Configuration
            .getInstance()
            .load(
                applicationContext,
                PreferenceManager.getDefaultSharedPreferences(
                    applicationContext,
                ),
            )

        mapView?.setTileSource(TileSourceFactory.WIKIMEDIA)
        mapView?.setTilesScaledToDpi(true)
        mapView?.setMultiTouchControls(true)

        org.osmdroid.config.Configuration
            .getInstance()
            .additionalHttpRequestProperties["Referer"] =
            "http://maps.wikimedia.org/"
        mapView?.zoomController?.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView?.controller?.setZoom(ZOOM_LEVEL.toDouble())
        mapView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (markerImage.translationY == 0f) {
                        markerImage
                            .animate()
                            .translationY(-75f)
                            .setInterpolator(OvershootInterpolator())
                            .duration = 250
                    }
                }
                MotionEvent.ACTION_UP -> {
                    markerImage
                        .animate()
                        .translationY(0f)
                        .setInterpolator(OvershootInterpolator())
                        .duration = 250
                }
            }
            false
        }

        mapManager =
            LocationPickerMapManager(
                this,
                requireNotNull(mapView),
                markerImage,
                locationManager,
            )
        permissionManager =
            LocationPickerPermissionManager(
                this,
                locationManager,
                store,
                mapManager,
                { moveToCurrentLocation },
                { activity },
            )

        if (activity == "UploadActivity") {
            placeSelectedButton.visibility = View.GONE
            modifyLocationButton.visibility = View.VISIBLE
            removeLocationButton.visibility = View.VISIBLE
            showInMapButton.visibility = View.VISIBLE
            largeToolbarText.text = getString(R.string.image_location)
            smallToolbarText.text = getString(R.string.check_whether_location_is_correct)
            fabCenterOnLocation.visibility = View.GONE
            markerImage.visibility = View.GONE
            shadow.visibility = View.GONE
            cameraPosition?.let {
                mapManager.showSelectedLocationMarker(GeoPoint(it.latitude, it.longitude))
            }
        }
        setupMapView()
    }

    /**
     * Binds the location-picker views and their listeners.
     */
    private fun bindViewsAndListeners() {
        mapView = findViewById(R.id.map_view)
        markerImage = findViewById(R.id.location_picker_image_view_marker)
        tvAttribution = findViewById(R.id.tv_attribution)
        modifyLocationButton = findViewById(R.id.modify_location)
        removeLocationButton = findViewById(R.id.remove_location)
        showInMapButton = findViewById(R.id.show_in_map)
        showInMapButton.text = getString(R.string.show_in_map_app).uppercase(Locale.ROOT)
        shadow = findViewById(R.id.location_picker_image_view_shadow)
        findViewById<ImageView>(R.id.maplibre_place_picker_toolbar_back_button)
            .setOnClickListener { finish() }

        tvAttribution.text =
            HtmlCompat.fromHtml(
                getString(R.string.map_attribution),
                HtmlCompat.FROM_HTML_MODE_LEGACY,
            )
        tvAttribution.movementMethod = LinkMovementMethod.getInstance()

        val toolbar: ConstraintLayout = findViewById(R.id.location_picker_toolbar)
        WindowCompat
            .getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        toolbar.applyEdgeToEdgeTopPaddingInsets()
        largeToolbarText = findViewById(R.id.location_picker_toolbar_primary_text_view)
        smallToolbarText = findViewById(R.id.location_picker_toolbar_secondary_text_view)
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor))

        placeSelectedButton = findViewById(R.id.location_chosen_button)
        applyEdgeToEdgeBottomInsets(placeSelectedButton)
        placeSelectedButton.setOnClickListener { placeSelected() }

        fabCenterOnLocation = findViewById(R.id.center_on_gps)
        fabCenterOnLocation.setOnClickListener {
            moveToCurrentLocation = true
            permissionManager.requestLocationPermissions()
        }
    }

    private fun setupMapView() {
        val mapBottomLayout: ConstraintLayout = findViewById(R.id.map_bottom_layout)
        mapBottomLayout.applyEdgeToEdgeBottomPaddingInsets()

        permissionManager.requestLocationPermissions()

        // If location metadata is available, move map to that location.
        if (activity == "UploadActivity" || activity == "MediaActivity") {
            mapManager.moveMapToMediaLocation(cameraPosition)
        } else {
            // If location metadata is not available, move map to device GPS location.
            mapManager.moveMapToGPSLocation()
        }

        modifyLocationButton.setOnClickListener {
            placeSelectedButton.visibility = View.VISIBLE
            modifyLocationButton.visibility = View.GONE
            removeLocationButton.visibility = View.GONE
            showInMapButton.visibility = View.GONE
            markerImage.visibility = View.VISIBLE
            shadow.visibility = View.VISIBLE
            largeToolbarText.text = getString(R.string.choose_a_location)
            smallToolbarText.text = getString(R.string.pan_and_zoom_to_adjust)
            fabCenterOnLocation.visibility = View.VISIBLE
            mapManager.removeSelectedLocationMarker(cameraPosition)
            mapManager.moveMapToMediaLocation(cameraPosition)
        }
        removeLocationButton.setOnClickListener {
            DialogUtil.showAlertDialog(
                this,
                getString(R.string.remove_location_warning_title),
                getString(R.string.remove_location_warning_desc),
                getString(R.string.continue_message),
                getString(R.string.cancel),
                { removeLocationFromImage() },
                null,
            )
        }
        showInMapButton.setOnClickListener {
            mapManager.showInMapApp(activity, cameraPosition)
        }
        mapManager.applyDarkTheme(isDarkTheme, shadow)
    }

    /**
     * Removes location metadata from the image
     */
    private fun removeLocationFromImage() {
        media?.let {
            coordinateEditHelper
                .makeCoordinatesEdit(
                    applicationContext,
                    it,
                    "0.0",
                    "0.0",
                    "0.0f",
                )?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { _ ->
                    Timber.d("Coordinates removed from the image")
                }?.let { it1 ->
                    compositeDisposable.add(
                        it1,
                    )
                }
        }
        setResult(RESULT_OK, Intent())
        finish()
    }

    /**
     * Handles "Place Selected" action
     */
    private fun placeSelected() {
        if (activity == "NoLocationUploadActivity") {
            applicationKvStore.putString(
                LAST_LOCATION,
                "${mapView?.mapCenter?.latitude},${mapView?.mapCenter?.longitude}",
            )
            applicationKvStore.putString(LAST_ZOOM, mapView?.zoomLevelDouble?.toString()!!)
        }

        if (media == null) {
            val intent =
                Intent().apply {
                    putExtra(
                        LocationPickerConstants.MAP_CAMERA_POSITION,
                        CameraPosition(
                            mapView?.mapCenter?.latitude!!,
                            mapView?.mapCenter?.longitude!!,
                            14.0,
                        ),
                    )
                }
            setResult(RESULT_OK, intent)
        } else {
            updateCoordinates(
                mapView?.mapCenter?.latitude.toString(),
                mapView?.mapCenter?.longitude.toString(),
                "0.0f",
            )
        }

        finish()
    }

    /**
     * Updates image with new coordinates
     */
    fun updateCoordinates(
        latitude: String,
        longitude: String,
        accuracy: String,
    ) {
        media?.let {
            try {
                coordinateEditHelper
                    .makeCoordinatesEdit(
                        applicationContext,
                        it,
                        latitude,
                        longitude,
                        accuracy,
                    )?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe { _ ->
                        Timber.d("Coordinates updated")
                    }?.let { it1 ->
                        compositeDisposable.add(
                            it1,
                        )
                    }
            } catch (e: Exception) {
                if (e.localizedMessage == CsrfTokenClient.ANONYMOUS_TOKEN_MESSAGE) {
                    val username = sessionManager.userName
                    CommonsApplication
                        .BaseLogoutListener(
                            this,
                            getString(R.string.invalid_login_message),
                            username,
                        ).let {
                            CommonsApplication.instance.clearApplicationData(this, it)
                        }
                } else {
                    Timber.e(e, "Failed to update image coordinates")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        permissionManager.onRequestPermissionsResult(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    /**
     * Saves the state of the activity
     * @param outState Bundle
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        cameraPosition?.let {
            outState.putParcelable(CAMERA_POS, it)
        }

        activity?.let {
            outState.putString(ACTIVITY, it)
        }

        media?.let {
            outState.putParcelable("sMedia", it)
        }
    }
}
