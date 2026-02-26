package fr.free.nrw.commons.upload.mediaDetails

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.chrisbanes.photoview.PhotoView
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentUploadMediaDetailFragmentBinding
import fr.free.nrw.commons.edit.EditActivity
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.locationpicker.LocationPicker
import fr.free.nrw.commons.locationpicker.LocationPicker.getCameraPosition
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.SimilarImageDialogFragment
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadBaseFragment
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter.Companion.presenterCallback
import fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ImageUtils
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.utils.handleKeyboardInsets
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.Locale
import java.util.Objects
import javax.inject.Inject
import javax.inject.Named

class UploadMediaDetailFragment : UploadBaseFragment(), UploadMediaDetailsContract.View,
    UploadMediaDetailAdapter.EventListener {

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var startForEditActivityResult: ActivityResultLauncher<Intent>
    private lateinit var voiceInputResultLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var presenter: UploadMediaDetailsContract.UserActionListener

    @Inject
    @field:Named("default_preferences")
    lateinit var defaultKvStore: JsonKvStore

    @Inject
    lateinit var recentLanguagesDao: RecentLanguagesDao

    /**
     * True when user removes location from the current image
     */
    var hasUserRemovedLocation = false

    /**
     * True if location is added via the "missing location" popup dialog (which appears after
     * tapping "Next" if the picture has no geographical coordinates).
     */
    private var isMissingLocationDialog = false

    /**
     * showNearbyFound will be true, if any nearby location found that needs pictures and the nearby
     * popup is yet to be shown Used to show and check if the nearby found popup is already shown
     */
    private var showNearbyFound = false

    /**
     * nearbyPlace holds the detail of nearby place that need pictures, if any found
     */
    private var nearbyPlace: Place? = null
    private var uploadItem: UploadItem? = null

    /**
     * inAppPictureLocation: use location recorded while using the in-app camera if device camera
     * does not record it in the EXIF
     */
    var inAppPictureLocation: LatLng? = null

    /**
     * editableUploadItem : Storing the upload item before going to update the coordinates
     */
    private var editableUploadItem: UploadItem? = null

    private var _binding: FragmentUploadMediaDetailFragmentBinding? = null
    private val binding: FragmentUploadMediaDetailFragmentBinding get() = _binding!!

    private var basicKvStore: BasicKvStore? = null
    private val keyForShowingAlertDialog = "isNoNetworkAlertDialogShowing"
    internal var uploadableFile: UploadableFile? = null
    internal var place: Place? = null
    private lateinit var uploadMediaDetailAdapter: UploadMediaDetailAdapter
    var indexOfFragment = 0
    var isExpanded = true
    var fragmentCallback: UploadMediaDetailFragmentCallback? = null
        set(value) {
            field = value
            UploadMediaPresenter.presenterCallback = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null && uploadableFile == null) {
            uploadableFile = savedInstanceState.getParcelable(UPLOADABLE_FILE)
        }
        // Register the ActivityResultLauncher for LocationPickerActivity
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onCameraPosition(result)
        }
        startForEditActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onEditActivityResult)
        voiceInputResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::onVoiceInput)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadMediaDetailFragmentBinding.inflate(inflater, container, false)
        _binding!!.mediaDetailCardView.handleKeyboardInsets()
        // intialise the adapter early to prevent uninitialized access
        uploadMediaDetailAdapter = UploadMediaDetailAdapter(
            this,
            defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, "")!!,
            recentLanguagesDao, voiceInputResultLauncher
        )
        uploadMediaDetailAdapter.callback =
            UploadMediaDetailAdapter.Callback { titleStringID: Int, messageStringId: Int ->
                showInfoAlert(titleStringID, messageStringId)
            }
        uploadMediaDetailAdapter.eventListener = this
        binding.rvDescriptions.layoutManager = LinearLayoutManager(context)
        binding.rvDescriptions.adapter = uploadMediaDetailAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        basicKvStore = BasicKvStore(requireActivity(), "CurrentUploadImageQualities")

        // restore adapter items from savedInstanceState if available
        if (savedInstanceState != null) {
            val savedItems = savedInstanceState.getParcelableArrayList<UploadMediaDetail>(UPLOAD_MEDIA_DETAILS)
            Timber.d("Restoring state: savedItems size = %s", savedItems?.size ?: "null")
            if (savedItems != null && savedItems.isNotEmpty()) {
                uploadMediaDetailAdapter.items = savedItems
                // only call setUploadMediaDetails if indexOfFragment is valid
                if (fragmentCallback != null) {
                    indexOfFragment = fragmentCallback!!.getIndexInViewFlipper(this)
                    if (indexOfFragment >= 0) {
                        presenter.setUploadMediaDetails(uploadMediaDetailAdapter.items, indexOfFragment)
                        Timber.d("Restored and set upload media details for index %d", indexOfFragment)
                    } else {
                        Timber.w("Invalid indexOfFragment %d, skipping setUploadMediaDetails", indexOfFragment)
                    }
                } else {
                    Timber.w("fragmentCallback is null, skipping setUploadMediaDetails")
                }
            } else {
                // initialize with a default UploadMediaDetail if saved state is empty or null
                uploadMediaDetailAdapter.items = mutableListOf(UploadMediaDetail())
                Timber.d("Initialized default UploadMediaDetail due to empty or null savedItems")
            }
        } else {
            // intitialise with a default UploadMediaDetail for fresh fragment
            if (uploadMediaDetailAdapter.items.isEmpty()) {
                uploadMediaDetailAdapter.items = mutableListOf(UploadMediaDetail())
                Timber.d("Initialized default UploadMediaDetail for new fragment")
            }
        }

        if (fragmentCallback != null) {
            indexOfFragment = fragmentCallback!!.getIndexInViewFlipper(this)
            Timber.d("Fragment callback present, indexOfFragment = %d", indexOfFragment)
            initializeFragment()
        } else {
            Timber.w("Fragment callback is null, skipping initializeFragment")
        }

        try {
            if (indexOfFragment >= 0 && !presenter.getImageQuality(indexOfFragment, inAppPictureLocation, requireActivity())) {
                Timber.d("Image quality check failed, redirecting to MainActivity")
                startActivityWithFlags(
                    requireActivity(),
                    MainActivity::class.java,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during image quality check")
        }
    }

    internal fun initializeFragment() {
        if (_binding == null) {
            return
        }
        binding.tvTitle.text = getString(
            R.string.step_count, (indexOfFragment + 1),
            fragmentCallback!!.totalNumberOfSteps, getString(R.string.media_detail_step_title)
        )
        binding.tooltip.setOnClickListener {
            showInfoAlert(
                R.string.media_detail_step_title,
                R.string.media_details_tooltip
            )
        }
        presenter.onAttachView(this)
        presenter.setupBasicKvStoreFactory { BasicKvStore(requireActivity(), it) }

        presenter.receiveImage(uploadableFile, place, inAppPictureLocation)

        if (binding.backgroundImage is PhotoView) {
            (binding.backgroundImage as PhotoView).setMaximumScale(10.0f)
            Timber.d("PhotoView max scale set to 10.0f for deeper zoom.")
        }

        with (binding){
            if (indexOfFragment == 0) {
                btnPrevious.isEnabled = false
                btnPrevious.alpha = 0.5f
            } else {
                btnPrevious.isEnabled = true
                btnPrevious.alpha = 1.0f
            }

            // If the image EXIF data contains the location, show the map icon with a green tick
            if (inAppPictureLocation != null || (uploadableFile != null && uploadableFile!!.hasLocation())) {
                val mapTick =
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_available_20dp)
                locationImageView.setImageDrawable(mapTick)
                locationTextView.setText(R.string.edit_location)
            } else {
                // Otherwise, show the map icon with a red question mark
                val mapQuestionMark = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_map_not_available_20dp
                )
                locationImageView.setImageDrawable(mapQuestionMark)
                locationTextView.setText(R.string.add_location)
            }

            //If this is the last media, we have nothing to copy, lets not show the button
            btnCopySubsequentMedia.visibility =
                if (indexOfFragment == fragmentCallback!!.totalNumberOfSteps - 4) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            // lljtran only supports lossless JPEG rotation, so we disable editing for other formatts
            val filePath = uploadableFile?.getFilePath()?.toString() ?: ""
            val isJpeg = filePath.endsWith(".jpeg", ignoreCase = true)
                    || filePath.endsWith(".jpg", ignoreCase = true)
            llEditImage.visibility = if (isJpeg) View.VISIBLE else View.GONE

            btnNext.setOnClickListener { presenter.displayLocDialog(indexOfFragment, inAppPictureLocation, hasUserRemovedLocation) }
            btnPrevious.setOnClickListener { fragmentCallback?.onPreviousButtonClicked(indexOfFragment) }
            llEditImage.setOnClickListener { presenter.onEditButtonClicked(indexOfFragment) }
            llContainerTitle.setOnClickListener { expandCollapseLlMediaDetail(!isExpanded) }
            llLocationStatus.setOnClickListener { presenter.onMapIconClicked(indexOfFragment) }
            btnCopySubsequentMedia.setOnClickListener { onButtonCopyTitleDescToSubsequentMedia() }
        }

        attachImageViewScaleChangeListener()
    }

    /**
     * Attaches the scale change listener to the image view
     */
    private fun attachImageViewScaleChangeListener() {
        binding.backgroundImage.setOnScaleChangeListener { _: Float, _: Float, _: Float ->
            //Whenever the uses plays with the image, lets collapse the media detail container
            //only if it is not already collapsed, which resolves flickering of arrow
            if (isExpanded) {
                expandCollapseLlMediaDetail(false)
            }
        }
    }

    private fun showInfoAlert(titleStringID: Int, messageStringId: Int) {
        showAlertDialog(
            requireActivity(),
            getString(titleStringID),
            getString(messageStringId),
            getString(R.string.ok),
            null
        )
    }

    override fun showSimilarImageFragment(
        originalFilePath: String?, possibleFilePath: String?,
        similarImageCoordinates: ImageCoordinates?
    ) {
        val basicKvStore = BasicKvStore(requireActivity(), "IsAnyImageCancelled")
        if (!basicKvStore.getBoolean("IsAnyImageCancelled", false)) {
            val newFragment = SimilarImageDialogFragment()
            newFragment.isCancelable = false
            newFragment.callback = object : SimilarImageDialogFragment.Callback {
                override fun onPositiveResponse() {
                    Timber.d("positive response from similar image fragment")
                    presenter.useSimilarPictureCoordinates(
                        similarImageCoordinates!!,
                        indexOfFragment
                    )

                    // set the description text when user selects to use coordinate from the other image
                    // which was taken within 120s
                    // fixing: https://github.com/commons-app/apps-android-commons/issues/4700
                    uploadMediaDetailAdapter.items[0].descriptionText =
                        getString(R.string.similar_coordinate_description_auto_set)
                    updateMediaDetails(uploadMediaDetailAdapter.items)

                    // Replace the 'Add location' button with 'Edit location' button when user clicks
                    // yes in similar image dialog
                    // fixing: https://github.com/commons-app/apps-android-commons/issues/5669
                    val mapTick = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_map_available_20dp
                    )
                    binding.locationImageView.setImageDrawable(mapTick)
                    binding.locationTextView.setText(R.string.edit_location)
                }

                override fun onNegativeResponse() {
                    Timber.d("negative response from similar image fragment")
                }
            }
            newFragment.arguments = bundleOf(
                "originalImagePath" to originalFilePath,
                "possibleImagePath" to possibleFilePath
            )
            newFragment.show(childFragmentManager, "dialog")
        }
    }

    override fun onImageProcessed(uploadItem: UploadItem) {
        if (_binding == null) {
            return
        }
        binding.backgroundImage.setImageURI(uploadItem.mediaUri)
    }

    override fun onNearbyPlaceFound(
        uploadItem: UploadItem, place: Place?
    ) {
        nearbyPlace = place
        this.uploadItem = uploadItem
        showNearbyFound = true
        if (fragmentCallback == null) {
            return
        }
        if (indexOfFragment == 0) {
            if (UploadActivity.nearbyPopupAnswers!!.containsKey(nearbyPlace!!)) {
                val response = UploadActivity.nearbyPopupAnswers!![nearbyPlace!!]!!
                if (response) {
                    if (fragmentCallback != null) {
                        presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace, indexOfFragment)
                    }
                }
            } else {
                showNearbyPlaceFound(nearbyPlace!!)
            }
            showNearbyFound = false
        }
    }

    @SuppressLint("StringFormatInvalid") // To avoid the unwanted lint warning that string 'upload_nearby_place_found_description' is not of a valid format
    private fun showNearbyPlaceFound(place: Place) {
        val customLayout = layoutInflater.inflate(R.layout.custom_nearby_found, null)
        val nearbyFoundImage = customLayout.findViewById<ImageView>(R.id.nearbyItemImage)
        nearbyFoundImage.setImageURI(uploadItem!!.mediaUri)

        val activity: Activity? = activity

        if (activity is UploadActivity) {
            val isMultipleFilesSelected = activity.isMultipleFilesSelected

            // Determine the message based on the selection status
            val message = if (isMultipleFilesSelected) {
                // Use plural message if multiple files are selected
                String.format(
                    Locale.getDefault(),
                    getString(R.string.upload_nearby_place_found_description_plural),
                    place.getName()
                )
            } else {
                // Use singular message if only one file is selected
                String.format(
                    Locale.getDefault(),
                    getString(R.string.upload_nearby_place_found_description_singular),
                    place.getName()
                )
            }

            // Show the AlertDialog with the determined message
            showAlertDialog(
                requireActivity(),
                getString(R.string.upload_nearby_place_found_title),
                message,
                {
                    // Execute when user confirms the upload is of the specified place
                    UploadActivity.nearbyPopupAnswers!![place] = true
                    presenter.onUserConfirmedUploadIsOfPlace(place, indexOfFragment)
                },
                {
                    // Execute when user cancels the upload of the specified place
                    UploadActivity.nearbyPopupAnswers!![place] = false
                },
                customLayout
            )
        }
    }

    override fun showProgress(shouldShow: Boolean) {
        if (fragmentCallback == null) {
            return
        }
        fragmentCallback!!.showProgress(shouldShow)
    }

    override fun onImageValidationSuccess() {
        if (fragmentCallback == null) {
            return
        }
        fragmentCallback!!.onNextButtonClicked(indexOfFragment)
    }

    /**
     * This method gets called whenever the next/previous button is pressed
     */
    override fun onBecameVisible() {
        super.onBecameVisible()
        if (fragmentCallback == null) {
            return
        }
        presenter.fetchTitleAndDescription(indexOfFragment)
        if (showNearbyFound) {
            if (UploadActivity.nearbyPopupAnswers!!.containsKey(nearbyPlace!!)) {
                val response = UploadActivity.nearbyPopupAnswers!![nearbyPlace!!]!!
                if (response) {
                    presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace, indexOfFragment)
                }
            } else {
                showNearbyPlaceFound(nearbyPlace!!)
            }
            showNearbyFound = false
        }
    }

    override fun showMessage(stringResourceId: Int, colorResourceId: Int) =
        showLongToast(requireContext(), stringResourceId)

    override fun showMessage(message: String, colorResourceId: Int) =
        showLongToast(requireContext(), message)

    override fun showDuplicatePicturePopup(uploadItem: UploadItem) {
        if (defaultKvStore.getBoolean("showDuplicatePicturePopup", true)) {
            val uploadTitleFormat = getString(R.string.upload_title_duplicate)
            val checkBoxView = View
                .inflate(activity, R.layout.nearby_permission_dialog, null)
            val checkBox = checkBoxView.findViewById<View>(R.id.never_ask_again) as CheckBox
            checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    defaultKvStore.putBoolean("showDuplicatePicturePopup", false)
                }
            }
            showAlertDialog(
                requireActivity(),
                getString(R.string.duplicate_file_name),
                String.format(
                    Locale.getDefault(),
                    uploadTitleFormat,
                    uploadItem.filename
                ),
                getString(R.string.upload),
                getString(R.string.cancel),
                {
                    uploadItem.imageQuality = ImageUtils.IMAGE_KEEP
                    onImageValidationSuccess()
                }, null,
                checkBoxView
            )
        } else {
            uploadItem.imageQuality = ImageUtils.IMAGE_KEEP
            onImageValidationSuccess()
        }
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Does nothing if there is network connectivity and then the user presses okay
     */
    override fun showConnectionErrorPopupForCaptionCheck() {
        showAlertDialog(requireActivity(),
            getString(R.string.upload_connection_error_alert_title),
            getString(R.string.upload_connection_error_alert_detail),
            getString(R.string.ok),
            getString(R.string.cancel_upload),
            {
                if (!isInternetConnectionEstablished(requireActivity())) {
                    showConnectionErrorPopupForCaptionCheck()
                }
            },
            {
                requireActivity().finish()
            })
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Recalls UploadMediaPresenter.getImageQuality for all the next upload items,
     * if there is network connectivity and then the user presses okay
     */
    override fun showConnectionErrorPopup() {
        try {
            val FLAG_ALERT_DIALOG_SHOWING = basicKvStore!!.getBoolean(
                keyForShowingAlertDialog, false
            )
            if (!FLAG_ALERT_DIALOG_SHOWING) {
                basicKvStore!!.putBoolean(keyForShowingAlertDialog, true)
                showAlertDialog(
                    requireActivity(),
                    getString(R.string.upload_connection_error_alert_title),
                    getString(R.string.upload_connection_error_alert_detail),
                    getString(R.string.ok),
                    getString(R.string.cancel_upload),
                    {
                        basicKvStore!!.putBoolean(keyForShowingAlertDialog, false)
                        if (isInternetConnectionEstablished(requireActivity())) {
                            val sizeOfUploads = basicKvStore!!.getInt(
                                UploadActivity.KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE
                            )
                            for (i in indexOfFragment until sizeOfUploads) {
                                presenter.getImageQuality(
                                    i,
                                    inAppPictureLocation,
                                    requireActivity()
                                )
                            }
                        } else {
                            showConnectionErrorPopup()
                        }
                    },
                    {
                        basicKvStore!!.putBoolean(keyForShowingAlertDialog, false)
                        requireActivity().finish()
                    },
                    null
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun showExternalMap(uploadItem: UploadItem) =
        goToLocationPickerActivity(uploadItem)

    /**
     * Launches the image editing activity to edit the specified UploadItem.
     *
     * @param uploadItem The UploadItem to be edited.
     *
     * This method is called to start the image editing activity for a specific UploadItem.
     * It sets the UploadItem as the currently editable item, creates an intent to launch the
     * EditActivity, and passes the image file path as an extra in the intent. The activity
     * is started using resultLauncher that handles the result in respective callback.
     */
    override fun showEditActivity(uploadItem: UploadItem) {
        editableUploadItem = uploadItem
        val intent = Intent(context, EditActivity::class.java)
        //used the mediaUri from the uploadItem.
        //now, if the image is edited, uploadItem.mediaUri points to the new rotated file.
        //if it is not edited, it points to the original.
        val currentPath = uploadItem.mediaUri?.path ?: uploadableFile?.getFilePath().toString()
        intent.putExtra("image", currentPath)
        startForEditActivityResult.launch(intent)
    }

    /**
     * Start Location picker activity. Show the location first then user can modify it by clicking
     * modify location button.
     * @param uploadItem current upload item
     */
    private fun goToLocationPickerActivity(uploadItem: UploadItem) {
        editableUploadItem = uploadItem
        var defaultLatitude = 37.773972
        var defaultLongitude = -122.431297
        var defaultZoom = 16.0

        /* Retrieve image location from EXIF if present or
           check if user has provided location while using the in-app camera.
           Use location of last UploadItem if none of them is available */
        val locationPickerIntent: Intent
        if (uploadItem.gpsCoords != null && uploadItem.gpsCoords!!
                .decLatitude != 0.0 && uploadItem.gpsCoords!!.decLongitude != 0.0
        ) {
            defaultLatitude = uploadItem.gpsCoords!!.decLatitude
            defaultLongitude = uploadItem.gpsCoords!!.decLongitude
            defaultZoom = uploadItem.gpsCoords!!.zoomLevel

            locationPickerIntent = LocationPicker.IntentBuilder()
                .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, defaultZoom))
                .activityKey("UploadActivity")
                .build(requireActivity())
        } else {
            if (defaultKvStore.getString(LAST_LOCATION) != null) {
                val locationLatLng = defaultKvStore.getString(LAST_LOCATION)!!
                    .split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                defaultLatitude = locationLatLng[0].toDouble()
                defaultLongitude = locationLatLng[1].toDouble()
            }
            if (defaultKvStore.getString(LAST_ZOOM) != null) {
                defaultZoom = defaultKvStore.getString(LAST_ZOOM)!!.toDouble()
            }

            locationPickerIntent = LocationPicker.IntentBuilder()
                .defaultLocation(CameraPosition(defaultLatitude, defaultLongitude, defaultZoom))
                .activityKey("NoLocationUploadActivity")
                .build(requireActivity())
        }
        startForResult.launch(locationPickerIntent)
    }

    private fun onCameraPosition(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            checkNotNull(result.data)
            val cameraPosition = getCameraPosition(
                result.data!!
            )

            if (cameraPosition != null) {
                val latitude = cameraPosition.latitude.toString()
                val longitude = cameraPosition.longitude.toString()
                val zoom = cameraPosition.zoom

                editLocation(latitude, longitude, zoom)
                // If isMissingLocationDialog is true, it means that the user has already tapped the
                // "Next" button, so go directly to the next step.
                if (isMissingLocationDialog) {
                    isMissingLocationDialog = false
                    presenter.displayLocDialog(
                        indexOfFragment,
                        inAppPictureLocation,
                        hasUserRemovedLocation
                    )
                }
            } else {
                // If camera position is null means location is removed by the user
                removeLocation()
            }
        }
    }

    private fun onVoiceInput(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultData = result.data!!.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            uploadMediaDetailAdapter.handleSpeechResult(resultData!![0])
        } else {
            Timber.e("Error %s", result.resultCode)
        }
    }

    private fun onEditActivityResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data!!.getStringExtra("editedImageFilePath")

            if (Objects.equals(result, "Error")) {
                Timber.e("Error in rotating image")
                return
            }
            try {
                if (_binding != null) {
                    binding.backgroundImage.setImageURI(Uri.fromFile(File(path!!)))
                }
                editableUploadItem!!.setContentAndMediaUri(Uri.fromFile(File(path!!)))
                fragmentCallback!!.changeThumbnail(
                    indexOfFragment,
                    path
                )
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    /**
     * Removes the location data from the image, by setting them to null
     */
    private fun removeLocation() {
        editableUploadItem!!.gpsCoords!!.decimalCoords = null
        try {
            val sourceExif = ExifInterface(
                uploadableFile!!.getFilePath()
            )
            val exifTags = arrayOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
            )

            for (tag in exifTags) {
                sourceExif.setAttribute(tag, null)
            }
            sourceExif.saveAttributes()

            val mapQuestion =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_not_available_20dp)

            if (_binding != null) {
                binding.locationImageView.setImageDrawable(mapQuestion)
                binding.locationTextView.setText(R.string.add_location)
            }

            editableUploadItem!!.gpsCoords!!.decLatitude = 0.0
            editableUploadItem!!.gpsCoords!!.decLongitude = 0.0
            editableUploadItem!!.gpsCoords!!.imageCoordsExists = false
            hasUserRemovedLocation = true

            Toast.makeText(context, getString(R.string.location_removed), Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Timber.d(e)
            Toast.makeText(
                context, "Location could not be removed due to internal error",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Update the old coordinates with new one
     * @param latitude new latitude
     * @param longitude new longitude
     */
    fun editLocation(latitude: String, longitude: String, zoom: Double) {
        editableUploadItem!!.gpsCoords!!.decLatitude = latitude.toDouble()
        editableUploadItem!!.gpsCoords!!.decLongitude = longitude.toDouble()
        editableUploadItem!!.gpsCoords!!.decimalCoords = "$latitude|$longitude"
        editableUploadItem!!.gpsCoords!!.imageCoordsExists = true
        editableUploadItem!!.gpsCoords!!.zoomLevel = zoom

        // Replace the map icon using the one with a green tick
        val mapTick = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_available_20dp)

        if (_binding != null) {
            binding.locationImageView.setImageDrawable(mapTick)
            binding.locationTextView.setText(R.string.edit_location)
        }

        Toast.makeText(context, getString(R.string.location_updated), Toast.LENGTH_LONG).show()
    }

    override fun updateMediaDetails(uploadMediaDetails: List<UploadMediaDetail>) {
        uploadMediaDetailAdapter.items = uploadMediaDetails
        showNearbyFound =
            showNearbyFound && (uploadMediaDetails.isEmpty() || listContainsEmptyDetails(
                uploadMediaDetails
            ))
    }

    /**
     * if the media details that come in here are empty
     * (empty caption AND empty description, with caption being the decider here)
     * this method allows usage of nearby place caption and description if any
     * else it takes the media details saved in prior for this picture
     * @param uploadMediaDetails saved media details,
     * ex: in case when "copy to subsequent media" button is clicked
     * for a previous image
     * @return boolean whether the details are empty or not
     */
    private fun listContainsEmptyDetails(uploadMediaDetails: List<UploadMediaDetail>): Boolean {
        for ((_, descriptionText, captionText) in uploadMediaDetails) {
            if (!TextUtils.isEmpty(captionText) && !TextUtils.isEmpty(
                    descriptionText
                )
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Showing dialog for adding location
     *
     * @param runnable proceed for verifying image quality
     */
    override fun displayAddLocationDialog(runnable: Runnable) {
        isMissingLocationDialog = true
        showAlertDialog(
            requireActivity(),
            getString(R.string.no_location_found_title),
            getString(R.string.no_location_found_message),
            getString(R.string.add_location),
            getString(R.string.skip_login),
            {
                presenter.onMapIconClicked(indexOfFragment)
            },
            runnable
        )
    }

    override fun showBadImagePopup(errorCode: Int, index: Int, uploadItem: UploadItem) {
        //If the error message is null, we will probably not show anything
        val activity = requireActivity()
        val errorMessageForResult = getErrorMessageForResult(activity, errorCode)
        if (errorMessageForResult.isNotEmpty()) {
            showAlertDialog(
                activity,
                activity.getString(R.string.upload_problem_image),
                errorMessageForResult,
                activity.getString(R.string.upload),
                activity.getString(R.string.cancel),
                {
                    showProgress(false)
                    uploadItem.imageQuality = IMAGE_OK
                    uploadItem.hasInvalidLocation = false // Reset invalid location flag when user confirms upload
                },
                {
                    presenterCallback!!.deletePictureAtIndex(index)
                }
            )?.setCancelable(false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDetachView()
    }

    fun expandCollapseLlMediaDetail(shouldExpand: Boolean) {
        if (_binding == null) {
            return
        }
        binding.llContainerMediaDetail.visibility =
            if (shouldExpand) View.VISIBLE else View.GONE
        isExpanded = !isExpanded
        binding.ibExpandCollapse.rotation = binding.ibExpandCollapse.rotation + 180
    }

    override fun onPrimaryCaptionTextChange(isNotEmpty: Boolean) {
        if (_binding == null) {
            return
        }
        binding.btnCopySubsequentMedia.isEnabled = isNotEmpty
        binding.btnCopySubsequentMedia.isClickable = isNotEmpty
        binding.btnCopySubsequentMedia.alpha = if (isNotEmpty) 1.0f else 0.5f
        binding.btnNext.isEnabled = isNotEmpty
        binding.btnNext.isClickable = isNotEmpty
        binding.btnNext.alpha = if (isNotEmpty) 1.0f else 0.5f
    }

    /**
     * Adds new language item to RecyclerView
     */
    override fun addLanguage() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.isManuallyAdded = true //This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail)
        binding.rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.itemCount - 1)
    }

    fun onButtonCopyTitleDescToSubsequentMedia() {
        presenter.copyTitleAndDescriptionToSubsequentMedia(indexOfFragment)
        Toast.makeText(context, R.string.copied_successfully, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (uploadableFile != null) {
            outState.putParcelable(UPLOADABLE_FILE, uploadableFile)
        }
        outState.putParcelableArrayList(
            UPLOAD_MEDIA_DETAILS,
            ArrayList(uploadMediaDetailAdapter.items)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    interface UploadMediaDetailFragmentCallback : Callback {
        fun deletePictureAtIndex(index: Int)

        fun changeThumbnail(index: Int, uri: String)
    }

    companion object {
        /**
         * A key for applicationKvStore. By this key we can retrieve the location of last UploadItem ex.
         * 12.3433,54.78897 from applicationKvStore.
         */
        const val LAST_LOCATION: String = "last_location_while_uploading"
        const val LAST_ZOOM: String = "last_zoom_level_while_uploading"
        const val UPLOADABLE_FILE: String = "uploadable_file"
        const val UPLOAD_MEDIA_DETAILS: String = "upload_media_detail_adapter"
    }
}
