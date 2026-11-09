package fr.free.nrw.commons.upload.mediaDetails

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.SimilarImageDialogFragment
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter.Companion.presenterCallback
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ImageUtils
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import timber.log.Timber
import java.util.Locale

/** Presents confirmation and validation dialogs for the current upload step. */
internal class UploadMediaDetailDialogs : UploadMediaDetailsContract.Dialogs {
    lateinit var view: UploadMediaDetailView
    private val fragment get() = view.fragment
    private val binding get() = view.bindingProvider()!!
    private val uploadMediaDetailAdapter get() = view.adapterProvider()
    private val basicKvStore get() = view.storeProvider()
    private val keyForShowingAlertDialog = "isNoNetworkAlertDialogShowing"

    /** Displays the similar image confirmation dialog. */
    override fun showSimilarImageFragment(
        originalFilePath: String?,
        possibleFilePath: String?,
        similarImageCoordinates: ImageCoordinates?,
    ) {
        val basicKvStore = BasicKvStore(fragment.requireActivity(), "IsAnyImageCancelled")
        if (!basicKvStore.getBoolean("IsAnyImageCancelled", false)) {
            val newFragment = SimilarImageDialogFragment()
            newFragment.isCancelable = false
            newFragment.callback =
                object : SimilarImageDialogFragment.Callback {
                    override fun onPositiveResponse() {
                        Timber.d("positive response from similar image fragment")
                        fragment.presenter.useSimilarPictureCoordinates(
                            similarImageCoordinates!!,
                            fragment.indexOfFragment,
                        )

                        // set the description text when user selects to use coordinate from the other image
                        // which was taken within 120s
                        // fixing: https://github.com/commons-app/apps-android-commons/issues/4700
                        uploadMediaDetailAdapter.items[0].descriptionText =
                            fragment.getString(R.string.similar_coordinate_description_auto_set)
                        view.metadata.updateMediaDetails(uploadMediaDetailAdapter.items)

                        // Replace the 'Add location' button with 'Edit location' button when user clicks
                        // yes in similar image dialog
                        // fixing: https://github.com/commons-app/apps-android-commons/issues/5669
                        val mapTick =
                            ContextCompat.getDrawable(
                                fragment.requireContext(),
                                R.drawable.ic_map_available_20dp,
                            )
                        binding.locationImageView.setImageDrawable(mapTick)
                        binding.locationTextView.setText(R.string.edit_location)
                    }

                    override fun onNegativeResponse() {
                        Timber.d("negative response from similar image fragment")
                    }
                }
            newFragment.arguments =
                bundleOf(
                    "originalImagePath" to originalFilePath,
                    "possibleImagePath" to possibleFilePath,
                )
            newFragment.show(fragment.childFragmentManager, "dialog")
        }
    }

    // To avoid the unwanted lint warning that string 'upload_nearby_place_found_description' is not of a valid format

    /** Displays the nearby place confirmation dialog. */
    @SuppressLint("StringFormatInvalid")
    fun showNearbyPlaceFound(
        place: Place,
        uploadItem: UploadItem?,
    ) {
        val customLayout = fragment.layoutInflater.inflate(R.layout.custom_nearby_found, null)
        val nearbyFoundImage = customLayout.findViewById<ImageView>(R.id.nearbyItemImage)
        nearbyFoundImage.setImageURI(uploadItem!!.mediaUri)

        val activity: Activity? = fragment.activity

        if (activity is UploadActivity) {
            val isMultipleFilesSelected = activity.isMultipleFilesSelected

            // Determine the message based on the selection status
            val message =
                if (isMultipleFilesSelected) {
                    // Use plural message if multiple files are selected
                    String.format(
                        Locale.getDefault(),
                        fragment.getString(R.string.upload_nearby_place_found_description_plural),
                        place.getName(),
                    )
                } else {
                    // Use singular message if only one file is selected
                    String.format(
                        Locale.getDefault(),
                        fragment.getString(R.string.upload_nearby_place_found_description_singular),
                        place.getName(),
                    )
                }

            // Show the AlertDialog with the determined message
            showAlertDialog(
                fragment.requireActivity(),
                fragment.getString(R.string.upload_nearby_place_found_title),
                message,
                {
                    // Execute when user confirms the upload is of the specified place
                    UploadActivity.nearbyPopupAnswers!![place] = true
                    fragment.presenter.onUserConfirmedUploadIsOfPlace(place, fragment.indexOfFragment)
                },
                {
                    // Execute when user cancels the upload of the specified place
                    UploadActivity.nearbyPopupAnswers!![place] = false
                },
                customLayout,
            )
        }
    }

    /** Displays the duplicate filename confirmation dialog. */
    override fun showDuplicatePicturePopup(uploadItem: UploadItem) {
        if (fragment.defaultKvStore.getBoolean("showDuplicatePicturePopup", true)) {
            val uploadTitleFormat = fragment.getString(R.string.upload_title_duplicate)
            val checkBoxView =
                View
                    .inflate(fragment.activity, R.layout.nearby_permission_dialog, null)
            val checkBox = checkBoxView.findViewById<View>(R.id.never_ask_again) as CheckBox
            checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    fragment.defaultKvStore.putBoolean("showDuplicatePicturePopup", false)
                }
            }
            showAlertDialog(
                fragment.requireActivity(),
                fragment.getString(R.string.duplicate_file_name),
                String.format(
                    Locale.getDefault(),
                    uploadTitleFormat,
                    uploadItem.filename,
                ),
                fragment.getString(R.string.upload),
                fragment.getString(R.string.cancel),
                {
                    uploadItem.imageQuality = ImageUtils.IMAGE_KEEP
                    view.onImageValidationSuccess()
                },
                null,
                checkBoxView,
            )
        } else {
            uploadItem.imageQuality = ImageUtils.IMAGE_KEEP
            view.onImageValidationSuccess()
        }
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Does nothing if there is network connectivity and then the user presses okay
     */
    override fun showConnectionErrorPopupForCaptionCheck() {
        showAlertDialog(
            fragment.requireActivity(),
            fragment.getString(R.string.upload_connection_error_alert_title),
            fragment.getString(R.string.upload_connection_error_alert_detail),
            fragment.getString(R.string.ok),
            fragment.getString(R.string.cancel_upload),
            {
                // Dismiss the dialog when offline instead of reopening it in a loop.
                // User can retry once connectivity is available.
            },
            {
                fragment.requireActivity().finish()
            },
        )
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Recalls UploadMediaPresenter.getImageQuality for all the next upload items,
     * if there is network connectivity and then the user presses okay
     */
    override fun showConnectionErrorPopup() {
        fragment.handleUploadExceptions(
            action = {
                val flagAlertDialogShowing =
                    basicKvStore!!.getBoolean(
                        keyForShowingAlertDialog,
                        false,
                    )
                if (!flagAlertDialogShowing) {
                    basicKvStore!!.putBoolean(keyForShowingAlertDialog, true)
                    showAlertDialog(
                        fragment.requireActivity(),
                        fragment.getString(R.string.upload_connection_error_alert_title),
                        fragment.getString(R.string.upload_connection_error_alert_detail),
                        fragment.getString(R.string.ok),
                        fragment.getString(R.string.cancel_upload),
                        {
                            basicKvStore!!.putBoolean(keyForShowingAlertDialog, false)
                            if (isInternetConnectionEstablished(fragment.requireActivity())) {
                                val sizeOfUploads =
                                    basicKvStore!!.getInt(
                                        UploadActivity.KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE,
                                    )
                                for (i in fragment.indexOfFragment until sizeOfUploads) {
                                    fragment.presenter.getImageQuality(
                                        i,
                                        fragment.inAppPictureLocation,
                                        fragment.requireActivity(),
                                    )
                                }
                            }
                        },
                        {
                            basicKvStore!!.putBoolean(keyForShowingAlertDialog, false)
                            fragment.requireActivity().finish()
                        },
                        null,
                    )
                }
            },
            onError = { e ->
                Timber.e(e)
            },
        )
    }

    /** Displays the image quality confirmation dialog. */
    override fun showBadImagePopup(
        errorCode: Int,
        index: Int,
        uploadItem: UploadItem,
    ) {
        // If the error message is null, we will probably not show anything
        val activity = fragment.requireActivity()
        val errorMessageForResult = getErrorMessageForResult(activity, errorCode)
        if (errorMessageForResult.isNotEmpty()) {
            showAlertDialog(
                activity,
                activity.getString(R.string.upload_problem_image),
                errorMessageForResult,
                activity.getString(R.string.upload),
                activity.getString(R.string.cancel),
                {
                    view.showProgress(false)
                    uploadItem.imageQuality = IMAGE_OK
                    uploadItem.hasInvalidLocation = false // Reset invalid location flag when user confirms upload
                },
                {
                    presenterCallback!!.deletePictureAtIndex(index)
                },
            )?.setCancelable(false)
        }
    }

    /** Displays help for a metadata field. */
    fun showInfoAlert(
        titleStringID: Int,
        messageStringId: Int,
    ) {
        showAlertDialog(
            fragment.requireActivity(),
            fragment.getString(titleStringID),
            fragment.getString(messageStringId),
            fragment.getString(R.string.ok),
            null,
        )
    }
}
