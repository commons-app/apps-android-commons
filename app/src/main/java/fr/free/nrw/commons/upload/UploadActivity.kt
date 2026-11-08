package fr.free.nrw.commons.upload

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.databinding.ActivityUploadBinding
import fr.free.nrw.commons.filepicker.Constants.RequestCodes
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.mwapi.UserClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.ThumbnailsAdapter.OnThumbnailDeletedListener
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Named

/** Hosts the lifecycle of the upload flow. */
class UploadActivity private constructor(
    private val uploadView: UploadActivityView,
) : BaseActivity(),
    UploadContract.View by uploadView,
    UploadBaseFragment.Callback by uploadView.navigation,
    OnThumbnailDeletedListener by uploadView.navigation {
    /** Creates an upload activity with its own flow state. */
    constructor() : this(UploadActivityView())

    init {
        uploadView.activity = this
    }

    @JvmField
    @Inject
    var contributionController: ContributionController? = null

    @JvmField
    @Inject
    @field:Named("default_preferences")
    var directKvStore: JsonKvStore? = null

    @JvmField
    @Inject
    var presenter: UploadContract.UserActionListener? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    @JvmField
    @Inject
    var userClient: UserClient? = null

    @JvmField
    @Inject
    var locationManager: LocationServiceManager? = null

    var store: BasicKvStore?
        get() = uploadView.store
        set(value) {
            uploadView.store = value
        }

    /** Returns if multiple files selected or not. */
    val isMultipleFilesSelected get() = uploadView.isMultipleFilesSelected

    /**
     * Get the value of the showPermissionDialog variable.
     *
     * @return `true` if Permission Dialog should be shown, `false` otherwise.
     *
     * Set the value of the showPermissionDialog variable.
     *
     * @property isShowPermissionsDialog `true` to indicate to show
     * Permissions Dialog if permissions are missing, `false` otherwise.
     *
     * A private boolean variable to control whether a permissions dialog should be shown
     * when necessary. Initially, it is set to `true`, indicating that the permissions dialog
     * should be displayed if permissions are missing and it is first time calling
     * `checkStoragePermissions` method.
     * This variable is used in the `checkStoragePermissions` method to determine whether to
     * show a permissions dialog to the user if the required permissions are not granted.
     * If `showPermissionsDialog` is set to `true` and the necessary permissions are missing,
     * a permissions dialog will be displayed to request the required permissions. If set
     * to `false`, the dialog won't be shown.
     *
     * @see UploadActivity.checkStoragePermissions
     */
    var isShowPermissionsDialog: Boolean
        get() = uploadView.isShowPermissionsDialog
        set(value) {
            uploadView.isShowPermissionsDialog = value
        }

    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: ActivityUploadBinding? = null
    internal val binding: ActivityUploadBinding get() = _binding!!
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure basicKvStoreFactory is always initialized before use
        presenter?.setupBasicKvStoreFactory { BasicKvStore(this@UploadActivity, it) }

        _binding = ActivityUploadBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(_binding!!.root, false)
        setContentView(binding.root)

        // Overrides the back button to make sure the user is prepared to lose their progress
        onBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showAlertDialog(
                        this@UploadActivity,
                        getString(R.string.back_button_warning),
                        getString(R.string.back_button_warning_desc),
                        getString(R.string.back_button_continue),
                        getString(R.string.back_button_warning),
                        null,
                        { finish() },
                        false,
                    )
                }
            }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        /*
         If Configuration of device is changed then get the new fragments
         created by the system and populate the fragments ArrayList
         */
        if (savedInstanceState != null) {
            uploadView.isFragmentsSaved = true
            uploadView.fragments =
                mutableListOf<UploadBaseFragment>().apply {
                    supportFragmentManager.fragments.forEach { fragment ->
                        add(fragment as UploadBaseFragment)
                    }
                }
        }

        uploadView.initProgressDialog()
        uploadView.navigation.initViewPager()
        uploadView.navigation.initThumbnailsRecyclerView()
        // And init other things you need to
        binding.rlContainerTitle.setOnClickListener { _: View? ->
            uploadView.navigation.onRlContainerTitleClicked()
        }
        nearbyPopupAnswers = mutableMapOf()
        // getting the current dpi of the device and if it is less than 320dp i.e. overlapping
        // threshold, thumbnails automatically minimizes
        val metrics = resources.displayMetrics
        val dpi = (metrics.widthPixels) / (metrics.density)
        if (dpi <= 321) {
            uploadView.navigation.onRlContainerTitleClicked()
        }
        if (hasPermission(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))) {
            locationManager!!.registerLocationManager()
        }
        locationManager!!.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
        locationManager!!.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
        store =
            BasicKvStore(this, STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE).apply {
                clearAll()
            }
        uploadView.input.checkStoragePermissions()
    }

    override fun onResume() {
        super.onResume()
        presenter!!.onAttachView(this)
        if (!uploadView.dialogs.isLoggedIn()) {
            uploadView.dialogs.askUserToLogIn()
        }
        checkBlockStatus()
    }

    override fun onStop() {
        // Resetting setImageCancelled to false
        uploadView.dialogs.setImageCancelled(false)
        super.onStop()
    }

    /**
     * Makes API call to check if user is blocked from Commons. If the user is blocked, a snackbar
     * is created to notify the user
     */
    private fun checkBlockStatus() {
        compositeDisposable.add(
            userClient!!
                .isUserBlockedFromCommons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter { result: Boolean? -> result!! }
                .subscribe { _: Boolean? ->
                    showAlertDialog(
                        this,
                        getString(R.string.block_notification_title),
                        getString(R.string.block_notification),
                        getString(R.string.ok),
                    ) { finish() }
                },
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val action = intent.action
        if (Intent.ACTION_SEND == action ||
            Intent.ACTION_SEND_MULTIPLE == action ||
            ContributionController.ACTION_INTERNAL_UPLOADS == action
        ) {
            // clear the old fragments
            val fm = supportFragmentManager
            if (fm.fragments.isNotEmpty()) {
                val tp = fm.beginTransaction()
                for (fragment in fm.fragments) {
                    tp.remove(fragment)
                }
                tp.commitNowAllowingStateLoss()
            }
            // wipe the shared model and the ui lists
            presenter?.cleanup()
            uploadView.uploadableFiles.clear()
            uploadView.fragments = null
            uploadView.uploadImagesAdapter = null
            uploadView.isFragmentsSaved = false
        }
        uploadView.input.checkStoragePermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        var areAllGranted = false
        if (requestCode == RequestCodes.STORAGE) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                for (i in grantResults.indices) {
                    val permission = permissions[i]
                    areAllGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        val showRationale = shouldShowRequestPermissionRationale(permission)
                        if (!showRationale) {
                            showAlertDialog(
                                this,
                                getString(R.string.storage_permissions_denied),
                                getString(R.string.unable_to_share_upload_item),
                                getString(R.string.ok),
                            ) { finish() }
                        } else {
                            showAlertDialog(
                                this,
                                getString(R.string.storage_permission_title),
                                getString(
                                    R.string.write_storage_permission_rationale_for_image_share,
                                ),
                                getString(R.string.ok),
                            ) { uploadView.input.checkStoragePermissions() }
                        }
                    }
                }

                if (areAllGranted) {
                    uploadView.steps.receiveSharedItems()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /** Clears the nearby place selection after reading the upload intent. */
    fun resetDirectPrefs() = directKvStore!!.remove(PLACE_OBJECT)

    /** Uses the Android activity implementation when closing the flow. */
    override fun finish() = super.finish()

    /** Shares one progress dialog between the presenter and fragment callbacks. */
    override fun showProgress(shouldShow: Boolean) = uploadView.showProgress(shouldShow)

    override fun onDestroy() {
        super.onDestroy()
        // Resetting all values in store by clearing them
        store!!.clearAll()
        if (isFinishing) {
            presenter?.cleanup()
        }
        presenter!!.onDetachView()
        compositeDisposable.clear()
        uploadView.fragments = null
        uploadView.uploadImagesAdapter = null
        if (uploadView.mediaLicenseFragment != null) {
            uploadView.mediaLicenseFragment!!.callback = null
        }
        if (uploadView.uploadCategoriesFragment != null) {
            uploadView.uploadCategoriesFragment!!.callback = null
        }
        onBackPressedCallback.remove()
    }

    companion object {
        internal var uploadIsOfAPlace = false
        const val EXTRA_FILES: String = "commons_image_extra"
        const val LOCATION_BEFORE_IMAGE_CAPTURE: String = "user_location_before_image_capture"
        const val IN_APP_CAMERA_UPLOAD: String = "in_app_camera_upload"

        /**
         * Stores all nearby places found and related users response for
         * each place while uploading media
         */
        @JvmField
        var nearbyPopupAnswers: MutableMap<Place, Boolean>? = null

        const val KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE: String = "CurrentUploadImagesSize"
        const val STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE: String = "CurrentUploadImageQualities"

        /**
         * Sets the flag indicating whether the upload is of a specific place.
         *
         * @param uploadOfAPlace a boolean value indicating whether the upload is of place.
         */
        @JvmStatic
        fun setUploadIsOfAPlace(uploadOfAPlace: Boolean) {
            uploadIsOfAPlace = uploadOfAPlace
        }
    }
}
