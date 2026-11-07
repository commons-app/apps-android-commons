package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import fr.free.nrw.commons.R
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter
import timber.log.Timber

/** Manages caption entry, description languages, and nearby place metadata. */
internal class UploadMediaMetadataController(
    private val view: UploadMediaDetailView,
) : UploadMediaDetailAdapter.EventListener {
    private val fragment get() = view.fragment
    private val currentBinding get() = view.bindingProvider()
    private val binding get() = currentBinding!!
    private val uploadMediaDetailAdapter get() = view.adapterProvider()
    private val dialogs get() = view.dialogs

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
     * Attaches the scale change listener to the image view
     */
    fun attachImageViewScaleChangeListener() {
        binding.backgroundImage.setOnScaleChangeListener { _: Float, _: Float, _: Float ->
            // Whenever the uses plays with the image, lets collapse the media detail container
            // only if it is not already collapsed, which resolves flickering of arrow
            if (fragment.isExpanded) {
                expandCollapseLlMediaDetail(false)
            }
        }
    }

    /** Handles the nearby place found for the current image. */
    fun onNearbyPlaceFound(
        uploadItem: UploadItem,
        place: Place?,
    ) {
        nearbyPlace = place
        this@UploadMediaMetadataController.uploadItem = uploadItem
        showNearbyFound = true
        if (fragment.fragmentCallback == null) {
            return
        }
        if (fragment.indexOfFragment != 0) {
            return
        }

        if (UploadActivity.nearbyPopupAnswers!!.containsKey(nearbyPlace!!)) {
            val response = UploadActivity.nearbyPopupAnswers!![nearbyPlace!!]!!
            if (response) {
                if (fragment.fragmentCallback != null) {
                    fragment.presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace, fragment.indexOfFragment)
                }
            }
        } else {
            dialogs.showNearbyPlaceFound(nearbyPlace!!, uploadItem)
        }
        showNearbyFound = false
    }

    /**
     * This method gets called whenever the next/previous button is pressed
     */
    fun onBecameVisible() {
        if (fragment.fragmentCallback == null) {
            return
        }
        fragment.presenter.fetchTitleAndDescription(fragment.indexOfFragment)
        if (showNearbyFound) {
            if (UploadActivity.nearbyPopupAnswers!!.containsKey(nearbyPlace!!)) {
                val response = UploadActivity.nearbyPopupAnswers!![nearbyPlace!!]!!
                if (response) {
                    fragment.presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace, fragment.indexOfFragment)
                }
            } else {
                dialogs.showNearbyPlaceFound(nearbyPlace!!, uploadItem)
            }
            showNearbyFound = false
        }
    }

    /** Applies the speech recognition result to the description. */
    fun onVoiceInput(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultData =
                result.data!!.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS,
                )
            uploadMediaDetailAdapter.handleSpeechResult(resultData!![0])
        } else {
            Timber.e("Error %s", result.resultCode)
        }
    }

    /** Displays saved captions and descriptions. */
    fun updateMediaDetails(uploadMediaDetails: List<UploadMediaDetail>) {
        uploadMediaDetailAdapter.items = uploadMediaDetails
        showNearbyFound =
            showNearbyFound &&
            (
                uploadMediaDetails.isEmpty() ||
                    listContainsEmptyDetails(
                        uploadMediaDetails,
                    )
            )
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
        for ((_, _, captionText) in uploadMediaDetails) {
            if (!TextUtils.isEmpty(captionText)) {
                return false
            }
        }
        return true
    }

    /** Updates the expanded state of the metadata panel. */
    fun expandCollapseLlMediaDetail(shouldExpand: Boolean) {
        if (currentBinding == null) {
            return
        }
        binding.llContainerMediaDetail.visibility =
            if (shouldExpand) View.VISIBLE else View.GONE
        fragment.isExpanded = !fragment.isExpanded
        binding.ibExpandCollapse.rotation = binding.ibExpandCollapse.rotation + HALF_TURN_DEGREES
    }

    /** Enables navigation when a primary caption is present. */
    override fun onPrimaryCaptionTextChange(isNotEmpty: Boolean) {
        if (currentBinding == null) {
            return
        }
        binding.btnCopySubsequentMedia.isEnabled = isNotEmpty
        binding.btnCopySubsequentMedia.isClickable = isNotEmpty
        binding.btnCopySubsequentMedia.alpha = if (isNotEmpty) 1.0f else DISABLED_ALPHA
        binding.btnNext.isEnabled = isNotEmpty
        binding.btnNext.isClickable = isNotEmpty
        binding.btnNext.alpha = if (isNotEmpty) 1.0f else DISABLED_ALPHA
    }

    /**
     * Adds new language item to RecyclerView
     */
    override fun addLanguage() {
        val uploadMediaDetail = UploadMediaDetail()
        uploadMediaDetail.isManuallyAdded = true // This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail)
        binding.rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.itemCount - 1)
    }

    /** Copies the current metadata to later upload items. */
    fun onButtonCopyTitleDescToSubsequentMedia() {
        fragment.presenter.copyTitleAndDescriptionToSubsequentMedia(fragment.indexOfFragment)
        Toast.makeText(fragment.context, R.string.copied_successfully, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val HALF_TURN_DEGREES = 180
        private const val DISABLED_ALPHA = 0.5f
    }
}
