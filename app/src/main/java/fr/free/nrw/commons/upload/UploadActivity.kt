package fr.free.nrw.commons.upload

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.work.ExistingWorkPolicy
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.databinding.ActivityUploadBinding
import fr.free.nrw.commons.filepicker.Constants.RequestCodes
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.mwapi.UserClient
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.upload.ThumbnailsAdapter.OnThumbnailDeletedListener
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.license.MediaLicenseFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.Companion.makeOneTimeWorkRequest
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.PermissionUtils.PERMISSIONS_STORAGE
import fr.free.nrw.commons.utils.PermissionUtils.checkPermissionsAndPerformAction
import fr.free.nrw.commons.utils.PermissionUtils.hasPartialAccess
import fr.free.nrw.commons.utils.PermissionUtils.hasPermission
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE_CATEGORY
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class UploadActivity : BaseActivity(), UploadContract.View, UploadBaseFragment.Callback,
    OnThumbnailDeletedListener {
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

    private var isTitleExpanded = true

    private var progressDialog: ProgressDialog? = null
    private var uploadImagesAdapter: UploadImageAdapter? = null
    private var fragments: MutableList<UploadBaseFragment>? = null
    private var uploadCategoriesFragment: UploadCategoriesFragment? = null
    private var depictsFragment: DepictsFragment? = null
    private var mediaLicenseFragment: MediaLicenseFragment? = null
    private var thumbnailsAdapter: ThumbnailsAdapter? = null
    var store: BasicKvStore? = null
    private var place: Place? = null
    private var prevLocation: LatLng? = null
    private var currLocation: LatLng? = null
    private var isInAppCameraUpload = false
    private var uploadableFiles: MutableList<UploadableFile> = mutableListOf()
    private var currentSelectedPosition = 0

    /**
     * Returns if multiple files selected or not.
     */
    /*
         Checks for if multiple files selected
         */
    var isMultipleFilesSelected: Boolean = false
        private set

    /**
     * Get the value of the showPermissionDialog variable.
     *
     * @return `true` if Permission Dialog should be shown, `false` otherwise.
     */
    /**
     * Set the value of the showPermissionDialog variable.
     *
     * @property isShowPermissionsDialog `true` to indicate to show
     * Permissions Dialog if permissions are missing, `false` otherwise.
     */
    /**
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
    var isShowPermissionsDialog: Boolean = true

    /**
     * Whether fragments have been saved.
     */
    private var isFragmentsSaved = false

    override val totalNumberOfSteps: Int
        get() = fragments!!.size

    override val isWLMUpload: Boolean
        get() = place != null && place!!.isMonument

    /**
     * Users may uncheck Location tag from the Manage EXIF tags setting any time.
     * So, their location must not be shared in this case.
     *
     */
    private val isLocationTagUncheckedInTheSettings: Boolean
        get() {
            val prefExifTags: Set<String> =
                defaultKvStore.getStringSet(Prefs.MANAGED_EXIF_TAGS)
            return !prefExifTags.contains(getString(R.string.exif_tag_location))
        }

    private var _binding: ActivityUploadBinding? = null
    private val binding: ActivityUploadBinding get() = _binding!!

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
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showAlertDialog(
                    this@UploadActivity,
                    getString(R.string.back_button_warning),
                    getString(R.string.back_button_warning_desc),
                    getString(R.string.back_button_continue),
                    getString(R.string.back_button_warning),
                    null
                ) {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        /*
         If Configuration of device is changed then get the new fragments
         created by the system and populate the fragments ArrayList
         */
        if (savedInstanceState != null) {
            isFragmentsSaved = true
            fragments = mutableListOf<UploadBaseFragment>().apply {
                supportFragmentManager.fragments.forEach { fragment ->
                    add(fragment as UploadBaseFragment)
                }
            }
        }

        init()
        binding.rlContainerTitle.setOnClickListener { _: View? -> onRlContainerTitleClicked() }
        nearbyPopupAnswers = mutableMapOf()
        //getting the current dpi of the device and if it is less than 320dp i.e. overlapping
        //threshold, thumbnails automatically minimizes
        val metrics = resources.displayMetrics
        val dpi = (metrics.widthPixels) / (metrics.density)
        if (dpi <= 321) {
            onRlContainerTitleClicked()
        }
        if (hasPermission(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))) {
            locationManager!!.registerLocationManager()
        }
        locationManager!!.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
        locationManager!!.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
        store = BasicKvStore(this, STORE_NAME_FOR_CURRENT_UPLOAD_IMAGE_SIZE).apply {
            clearAll()
        }
        checkStoragePermissions()
    }

    private fun init() {
        initProgressDialog()
        initViewPager()
        initThumbnailsRecyclerView()
        //And init other things you need to
    }

    private fun initProgressDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage(getString(R.string.please_wait))
        progressDialog!!.setCancelable(false)
    }

    private fun initThumbnailsRecyclerView() {
        binding.rvThumbnails.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL, false
        )
        thumbnailsAdapter = ThumbnailsAdapter { currentSelectedPosition }
        thumbnailsAdapter!!.onThumbnailDeletedListener = this
        binding.rvThumbnails.adapter = thumbnailsAdapter
    }

    private fun initViewPager() {
        uploadImagesAdapter = UploadImageAdapter(supportFragmentManager)
        binding.vpUpload.adapter = uploadImagesAdapter
        binding.vpUpload.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int, positionOffset: Float,
                positionOffsetPixels: Int
            ) = Unit

            override fun onPageSelected(position: Int) {
                currentSelectedPosition = position
                if (position >= uploadableFiles.size) {
                    binding.cvContainerTopCard.visibility = View.GONE
                } else {
                    thumbnailsAdapter!!.notifyDataSetChanged()
                    binding.cvContainerTopCard.visibility = View.VISIBLE
                }
            }

            override fun onPageScrollStateChanged(state: Int) = Unit
        })
    }

    override fun isLoggedIn(): Boolean = sessionManager!!.isUserLoggedIn

    override fun onResume() {
        super.onResume()
        presenter!!.onAttachView(this)
        if (!isLoggedIn()) {
            askUserToLogIn()
        }
        checkBlockStatus()
    }

    /**
     * Makes API call to check if user is blocked from Commons. If the user is blocked, a snackbar
     * is created to notify the user
     */
    protected fun checkBlockStatus() {
        compositeDisposable.add(
            userClient!!.isUserBlockedFromCommons()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .filter { result: Boolean? -> result!! }
                .subscribe { _: Boolean? ->
                    showAlertDialog(
                        this,
                        getString(R.string.block_notification_title),
                        getString(R.string.block_notification),
                        getString(R.string.ok)
                    ) { finish() }
                })
    }

    private fun checkStoragePermissions() {
        // The share intent provides files via content uris with temporary read permissions,
        // so we do not need to obtain storage permissions
        val action = intent.action
        if (Intent.ACTION_SEND == action || Intent.ACTION_SEND_MULTIPLE == action) {
            // Get the external items first
            receiveExternalSharedItems()
            receiveSharedItems()
            return
        }

        // Check if all required permissions are granted
        val hasAllPermissions = hasPermission(this, PERMISSIONS_STORAGE)
        val hasPartialAccess = hasPartialAccess(this)

        if (hasAllPermissions || hasPartialAccess) {
            // All required permissions are granted, so enable UI elements and perform actions
            receiveSharedItems()
            binding.cvContainerTopCard.visibility = View.VISIBLE
        } else {
            // Permissions are missing
            binding.cvContainerTopCard.visibility = View.INVISIBLE
            if (isShowPermissionsDialog) {
                checkPermissionsAndPerformAction(
                    this,
                    Runnable {
                        binding.cvContainerTopCard.visibility = View.VISIBLE
                        receiveSharedItems()
                    }, Runnable {
                        isShowPermissionsDialog = true
                        checkStoragePermissions()
                    },
                    R.string.storage_permission_title,
                    R.string.write_storage_permission_rationale_for_image_share,
                    *PERMISSIONS_STORAGE
                )
            }
        }
        /* If all permissions are not granted and a dialog is already showing on screen
         showPermissionsDialog will set to false making it not show dialog again onResume,
         but if user Denies any permission showPermissionsDialog will be to true
         and permissions dialog will be shown again.
         */
        isShowPermissionsDialog = hasAllPermissions
    }

    override fun onStop() {
        // Resetting setImageCancelled to false
        setImageCancelled(false)
        super.onStop()
    }

    override fun returnToMainActivity() = finish()

    /**
     * go to the uploadProgress activity to check the status of uploading
     */
    override fun goToUploadProgressActivity() =
        startActivity(Intent(this, UploadProgressActivity::class.java))

    /**
     * Show/Hide the progress dialog
     */
    override fun showProgress(shouldShow: Boolean) {
        if (shouldShow) {
            if (!progressDialog!!.isShowing) {
                progressDialog!!.show()
            }
        } else {
            if (progressDialog != null && !isFinishing) {
                progressDialog!!.dismiss()
            }
        }
    }

    override fun getIndexInViewFlipper(fragment: UploadBaseFragment?): Int =
        fragments!!.indexOf(fragment)

    override fun showMessage(messageResourceId: Int) {
        showLongToast(this, messageResourceId)
    }

    override fun getUploadableFiles(): List<UploadableFile> {
        return uploadableFiles
    }

    override fun showHideTopCard(shouldShow: Boolean) {
        binding.llContainerTopCard.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun onUploadMediaDeleted(index: Int) {
        fragments!!.removeAt(index) //Remove the corresponding fragment
        uploadableFiles.removeAt(index) //Remove the files from the list

        val isMediaDetailFragment = fragments!!.getOrNull(currentSelectedPosition)?.let {
            it is UploadMediaDetailFragment
        } ?: false
        if(!isMediaDetailFragment) {
            // Should hide the top card current fragment is not the media detail fragment
            showHideTopCard(false)
        }
        thumbnailsAdapter!!.notifyItemRemoved(index) //Notify the thumbnails adapter
        uploadImagesAdapter!!.notifyDataSetChanged() //Notify the ViewPager
    }

    override fun updateTopCardTitle() {
        binding.tvTopCardTitle.text = resources
            .getQuantityString(
                R.plurals.upload_count_title,
                uploadableFiles.size,
                uploadableFiles.size
            )
    }

    override fun makeUploadRequest() {
        makeOneTimeWorkRequest(
            applicationContext,
            ExistingWorkPolicy.APPEND_OR_REPLACE
        )
    }

    override fun askUserToLogIn() {
        Timber.d("current session is null, asking user to login")
        showLongToast(this, getString(R.string.user_not_logged_in))
        val loginIntent = Intent(this@UploadActivity, LoginActivity::class.java)
        startActivity(loginIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
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
                                getString(R.string.ok)
                            ) { finish() }
                        } else {
                            showAlertDialog(
                                this,
                                getString(R.string.storage_permission_title),
                                getString(
                                    R.string.write_storage_permission_rationale_for_image_share
                                ),
                                getString(R.string.ok)
                            ) { checkStoragePermissions() }
                        }
                    }
                }

                if (areAllGranted) {
                    receiveSharedItems()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun receiveSharedItems() {
        val intent = intent
        val action = intent.action
        if (ContributionController.ACTION_INTERNAL_UPLOADS == action) {
            receiveInternalSharedItems()
        }

        if (uploadableFiles.isEmpty()) {
            handleNullMedia()
        } else {
            //Show thumbnails
            if (uploadableFiles.size > 1) {
                if (!defaultKvStore.getBoolean("hasAlreadyLaunchedCategoriesDialog")) {
                    // If there is only file, no need to show the image thumbnails
                    showAlertDialogForCategories()
                }
                if (uploadableFiles.size > 3 &&
                    !defaultKvStore.getBoolean("hasAlreadyLaunchedBigMultiupload")
                ) {
                    showAlertForBattery()
                }
                thumbnailsAdapter!!.uploadableFiles = uploadableFiles
            } else {
                binding.llContainerTopCard.visibility = View.GONE
            }
            binding.tvTopCardTitle.text = resources
                .getQuantityString(
                    R.plurals.upload_count_title,
                    uploadableFiles.size,
                    uploadableFiles.size
                )


            if (fragments == null) {
                fragments = mutableListOf()
            }

            for (uploadableFile in uploadableFiles) {
                val uploadMediaDetailFragment = UploadMediaDetailFragment()

                // set fragment properties but defer initialization
                uploadMediaDetailFragment.uploadableFile = uploadableFile
                uploadMediaDetailFragment.place = place
                uploadMediaDetailFragment.inAppPictureLocation = if (!uploadIsOfAPlace) {
                    handleLocation()
                    currLocation
                } else {
                    currLocation
                }

                val uploadMediaDetailFragmentCallback: UploadMediaDetailFragmentCallback =
                    object : UploadMediaDetailFragmentCallback {
                        override fun deletePictureAtIndex(index: Int) {
                            store!!.putInt(
                                KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE,
                                (store!!.getInt(KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE) - 1)
                            )
                            presenter!!.deletePictureAtIndex(index)
                        }

                        /**
                         * Changes the thumbnail of an UploadableFile at the specified index.
                         * This method updates the list of uploadableFiles by replacing the UploadableFile
                         * at the given index with a new UploadableFile created from the provided file path.
                         * After updating the list, it notifies the RecyclerView's adapter to refresh its data,
                         * ensuring that the thumbnail change is reflected in the UI.
                         *
                         * @param index The index of the UploadableFile to be updated.
                         * @param uri The file path of the new thumbnail image.
                         */
                        override fun changeThumbnail(index: Int, uri: String) {
                            uploadableFiles.removeAt(index)
                            uploadableFiles.add(index, UploadableFile(File(uri)))
                            binding.rvThumbnails.adapter!!.notifyDataSetChanged()
                        }

                        override fun onNextButtonClicked(index: Int) {
                            this@UploadActivity.onNextButtonClicked(index)
                        }

                        override fun onPreviousButtonClicked(index: Int) {
                            this@UploadActivity.onPreviousButtonClicked(index)
                        }

                        override fun showProgress(shouldShow: Boolean) {
                            this@UploadActivity.showProgress(shouldShow)
                        }

                        override fun getIndexInViewFlipper(fragment: UploadBaseFragment?): Int {
                            return fragments!!.indexOf(fragment)
                        }

                        override val totalNumberOfSteps: Int
                            get() = fragments!!.size

                        override val isWLMUpload: Boolean
                            get() = place != null && place!!.isMonument
                    }

                if (isFragmentsSaved) {
                    val fragment = fragments!![0] as UploadMediaDetailFragment?
                    fragment!!.fragmentCallback = uploadMediaDetailFragmentCallback
                    fragment.initializeFragment()
                } else {
                    uploadMediaDetailFragment.fragmentCallback = uploadMediaDetailFragmentCallback
                    fragments!!.add(uploadMediaDetailFragment)
                }
            }

            // unregister location manager after loop if needed
            if (!uploadIsOfAPlace) {
                locationManager!!.unregisterLocationManager()
            }

            // If fragments are not created, create them and add them to the fragments ArrayList
            if (!isFragmentsSaved) {
                uploadCategoriesFragment = UploadCategoriesFragment()
                if (place != null) {
                    val categoryBundle = Bundle()
                    categoryBundle.putString(SELECTED_NEARBY_PLACE_CATEGORY, place!!.category)
                    uploadCategoriesFragment!!.arguments = categoryBundle
                }

                uploadCategoriesFragment!!.callback = this

                depictsFragment = DepictsFragment()
                val placeBundle = Bundle()
                placeBundle.putParcelable(SELECTED_NEARBY_PLACE, place)
                depictsFragment!!.arguments = placeBundle
                depictsFragment!!.callback = this

                mediaLicenseFragment = MediaLicenseFragment()
                mediaLicenseFragment!!.callback = this

                fragments!!.add(depictsFragment!!)
                fragments!!.add(uploadCategoriesFragment!!)
                fragments!!.add(mediaLicenseFragment!!)
            } else {
                for (i in 1 until fragments!!.size) {
                    fragments!![i].callback = object : UploadBaseFragment.Callback {
                        override fun onNextButtonClicked(index: Int) {
                            if (index < fragments!!.size - 1) {
                                binding.vpUpload.setCurrentItem(index + 1, false)
                                fragments!![index + 1].onBecameVisible()
                                (binding.rvThumbnails.layoutManager as LinearLayoutManager)
                                    .scrollToPositionWithOffset(
                                        if ((index > 0)) index - 1 else 0,
                                        0
                                    )
                            } else {
                                presenter!!.handleSubmit()
                            }
                        }

                        override fun onPreviousButtonClicked(index: Int) {
                            if (index != 0) {
                                binding.vpUpload.setCurrentItem(index - 1, true)
                                fragments!![index - 1].onBecameVisible()
                                (binding.rvThumbnails.layoutManager as LinearLayoutManager)
                                    .scrollToPositionWithOffset(
                                        if ((index > 3)) index - 2 else 0,
                                        0
                                    )
                            }
                        }

                        override fun showProgress(shouldShow: Boolean) {
                            if (shouldShow) {
                                if (!progressDialog!!.isShowing) {
                                    progressDialog!!.show()
                                }
                            } else {
                                if (progressDialog != null && !isFinishing) {
                                    progressDialog!!.dismiss()
                                }
                            }
                        }

                        override fun getIndexInViewFlipper(fragment: UploadBaseFragment?): Int {
                            return fragments!!.indexOf(fragment)
                        }

                        override val totalNumberOfSteps: Int
                            get() = fragments!!.size

                        override val isWLMUpload: Boolean
                            get() = place != null && place!!.isMonument
                    }
                }
            }

            uploadImagesAdapter!!.fragments = fragments!!
            binding.vpUpload.offscreenPageLimit = fragments!!.size
        }
        // Saving size of uploadableFiles
        store!!.putInt(KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE, uploadableFiles.size)
    }

    /**
     * Changes current image when one image upload is cancelled, to highlight next image in the top
     * thumbnail.
     * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
     *
     * @param index Index of image to be removed
     * @param maxSize Max size of the `uploadableFiles`
     */
    override fun highlightNextImageOnCancelledImage(index: Int, maxSize: Int) {
        if (index < maxSize) {
            binding.vpUpload.setCurrentItem(index + 1, false)
            binding.vpUpload.setCurrentItem(index, false)
        }
    }

    /**
     * Used to check if user has cancelled upload of any image in current upload
     * so that location compare doesn't show up again in same upload.
     * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
     *
     * @param isCancelled Is true when user has cancelled upload of any image in current upload
     */
    override fun setImageCancelled(isCancelled: Boolean) {
        val basicKvStore = BasicKvStore(this, "IsAnyImageCancelled")
        basicKvStore.putBoolean("IsAnyImageCancelled", isCancelled)
    }

    /**
     * Calculate the difference between current location and
     * location recorded before capturing the image
     *
     */
    private fun getLocationDifference(currLocation: LatLng, prevLocation: LatLng?): Float {
        if (prevLocation == null) {
            return 0.0f
        }
        val distance = FloatArray(2)
        Location.distanceBetween(
            currLocation.latitude, currLocation.longitude,
            prevLocation.latitude, prevLocation.longitude, distance
        )
        return distance[0]
    }

    private fun receiveExternalSharedItems() {
        uploadableFiles = contributionController!!.handleExternalImagesPicked(this, intent).toMutableList()
    }

    private fun receiveInternalSharedItems() {
        val intent = intent
        Timber.d("Intent has EXTRA_FILES: ${EXTRA_FILES}")
        uploadableFiles = try {
            // Check if intent has the extra before trying to read it
            if (!intent.hasExtra(EXTRA_FILES)) {
                Timber.w("No EXTRA_FILES found in intent")
                mutableListOf()
            } else {
                // Try to get the files as Parcelable array
                val files = if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(EXTRA_FILES, UploadableFile::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<UploadableFile>(EXTRA_FILES)
                }

                // Convert to mutable list or return empty list if null
                files?.toMutableList() ?: run {
                    Timber.w("Files array was null")
                    mutableListOf()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading files from intent")
            mutableListOf()
        }

        // Log the result for debugging
        isMultipleFilesSelected = uploadableFiles.size > 1
        Timber.i("Received files count: ${uploadableFiles.size}")
        uploadableFiles.forEachIndexed { index, file ->
            Timber.d("File $index path: ${file.getFilePath()}")
        }

        // Handle other extras with null safety
        place = try {
            if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(PLACE_OBJECT, Place::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(PLACE_OBJECT)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading place")
            null
        }

        prevLocation = try {
            if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(LOCATION_BEFORE_IMAGE_CAPTURE, LatLng::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(LOCATION_BEFORE_IMAGE_CAPTURE)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading location")
            null
        }

        isInAppCameraUpload = intent.getBooleanExtra(IN_APP_CAMERA_UPLOAD, false)
        resetDirectPrefs()
    }

    fun resetDirectPrefs() = directKvStore!!.remove(PLACE_OBJECT)

    /**
     * Handle null URI from the received intent.
     * Current implementation will simply show a toast and finish the upload activity.
     */
    private fun handleNullMedia() {
        showLongToast(this, R.string.error_processing_image)
        finish()
    }


    override fun showAlertDialog(messageResourceId: Int, onPositiveClick: Runnable) {
        showAlertDialog(
            this,
            "",
            getString(messageResourceId),
            getString(R.string.ok),
            onPositiveClick
        )
    }

    override fun onNextButtonClicked(index: Int) {
        if (index < fragments!!.size - 1) {
            // Hide the keyboard before navigating to Media License screen
            val isUploadCategoriesFragment = fragments!!.getOrNull(index)?.let {
                it is UploadCategoriesFragment
            } ?: false
            if (isUploadCategoriesFragment) {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                currentFocus?.let { focusedView ->
                    inputMethodManager.hideSoftInputFromWindow(
                        focusedView.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
            }
            binding.vpUpload.setCurrentItem(index + 1, false)
            fragments!![index + 1].onBecameVisible()
            (binding.rvThumbnails.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(if ((index > 0)) index - 1 else 0, 0)
            if (index < fragments!!.size - 4) {
                // check image quality if next image exists
                presenter!!.checkImageQuality(index + 1)
            }
        } else {
            presenter!!.handleSubmit()
        }
    }

    override fun onPreviousButtonClicked(index: Int) {
        if (index != 0) {
            binding.vpUpload.setCurrentItem(index - 1, true)
            fragments!![index - 1].onBecameVisible()
            (binding.rvThumbnails.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(if ((index > 3)) index - 2 else 0, 0)
            if ((index != 1) && ((index - 1) < uploadableFiles.size)) {
                // Shows the top card if it was hidden because of the last image being deleted and
                // now the user has hit previous button to go back to the media details
                showHideTopCard(true)
            }
        }
    }

    override fun onThumbnailDeleted(position: Int) {
        presenter!!.deletePictureAtIndex(position)
        thumbnailsAdapter?.notifyDataSetChanged()
    }

    /**
     * The adapter used to show image upload intermediate fragments
     */
    private class UploadImageAdapter(fragmentManager: FragmentManager) :
        FragmentStatePagerAdapter(fragmentManager) {
        var fragments: List<UploadBaseFragment> = mutableListOf()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getItemPosition(item: Any): Int {
            return POSITION_NONE
        }
    }


    private fun onRlContainerTitleClicked() {
        binding.rvThumbnails.visibility =
            if (isTitleExpanded) View.GONE else View.VISIBLE
        isTitleExpanded = !isTitleExpanded
        binding.ibToggleTopCard.rotation += 180
    }

    override fun onDestroy() {
        super.onDestroy()
        // Resetting all values in store by clearing them
        store!!.clearAll()
        presenter!!.onDetachView()
        compositeDisposable.clear()
        fragments = null
        uploadImagesAdapter = null
        if (mediaLicenseFragment != null) {
            mediaLicenseFragment!!.callback = null
        }
        if (uploadCategoriesFragment != null) {
            uploadCategoriesFragment!!.callback = null
        }
        onBackPressedCallback.remove()
    }

    /**
     * If the user uploads more than 1 file informs that
     * depictions/categories apply to all pictures of a multi upload.
     * This method takes no arguments and does not return any value.
     * It shows the AlertDialog and continues the flow of uploads.
     */
    private fun showAlertDialogForCategories() {
        UploadMediaPresenter.isCategoriesDialogShowing = true
        // Inflate the custom layout
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.activity_upload_categories_dialog, null)
        val checkBox = view.findViewById<CheckBox>(R.id.categories_checkbox)
        // Create the alert dialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(view)
            .setTitle(getString(R.string.multiple_files_depiction_header))
            .setMessage(getString(R.string.multiple_files_depiction))
            .setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                if (checkBox.isChecked) {
                    // Save the user's choice to not show the dialog again
                    defaultKvStore.putBoolean("hasAlreadyLaunchedCategoriesDialog", true)
                }
                presenter!!.checkImageQuality(0)
                UploadMediaPresenter.isCategoriesDialogShowing = false
            }
            .setNegativeButton("", null)
            .create()
        alertDialog.show()
    }


    /** Suggest users to turn battery optimisation off when uploading
     * more than a few files. That's because we have noticed that
     * many-files uploads have a much higher probability of failing
     * than uploads with less files. Show the dialog for Android 6
     * and above as the ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
     * intent was added in API level 23
     */
    private fun showAlertForBattery() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            // When battery-optimisation dialog is shown don't show the image quality dialog
            UploadMediaPresenter.isBatteryDialogShowing = true
            showAlertDialog(
                this,
                getString(R.string.unrestricted_battery_mode),
                getString(R.string.suggest_unrestricted_mode),
                getString(R.string.title_activity_settings),
                getString(R.string.cancel),
                {
                    /* Since opening the right settings page might be device dependent, using
                      https://github.com/WaseemSabir/BatteryPermissionHelper
                      directly appeared like a promising idea.
                      However, this simply closed the popup and did not make
                      the settings page appear on a Pixel as well as a Xiaomi device.
                      Used the standard intent instead of using this library as
                      it shows a list of all the apps on the device and allows users to
                      turn battery optimisation off.
                    */
                    val batteryOptimisationSettingsIntent = Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    )
                    startActivity(batteryOptimisationSettingsIntent)

                    // calling checkImageQuality after battery dialog is interacted with
                    // so that 2 dialogs do not pop up simultaneously
                    UploadMediaPresenter.isBatteryDialogShowing = false
                },
                {
                    UploadMediaPresenter.isBatteryDialogShowing = false
                }
            )
            defaultKvStore.putBoolean("hasAlreadyLaunchedBigMultiupload", true)
        }
    }

    /**
     * If the permission for Location is turned on and certain
     * conditions are met, returns current location of the user.
     */
    private fun handleLocation() {
        val locationPermissionsHelper = LocationPermissionsHelper(
            this, locationManager!!, null
        )
        if (locationPermissionsHelper.isLocationAccessToAppsTurnedOn()) {
            currLocation = locationManager!!.getLastLocation()
        }

        if (currLocation != null) {
            val locationDifference = getLocationDifference(currLocation!!, prevLocation)
            val isLocationTagUnchecked = isLocationTagUncheckedInTheSettings
            /* Remove location if the user has unchecked the Location EXIF tag in the
                       Manage EXIF Tags setting or turned "Record location for in-app shots" off.
                       Also, location information is discarded if the difference between
                       current location and location recorded just before capturing the image
                       is greater than 100 meters */
            if (isLocationTagUnchecked || locationDifference > 100
                || !defaultKvStore.getBoolean("inAppCameraLocationPref")
                || !isInAppCameraUpload
            ) {
                currLocation = null
            }
        }
    }

    companion object {
        private var uploadIsOfAPlace = false
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
