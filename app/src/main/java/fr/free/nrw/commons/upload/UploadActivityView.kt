package fr.free.nrw.commons.upload

import android.app.ProgressDialog
import android.view.View
import androidx.work.ExistingWorkPolicy
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.license.MediaLicenseFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.makeOneTimeWorkRequest

/** Owns the state and presenter-facing view of the upload flow. */
internal class UploadActivityView(
    val dialogs: UploadActivityDialogs = UploadActivityDialogs(),
    val navigation: UploadNavigation = UploadNavigation(),
    val input: UploadInputController = UploadInputController(),
    val steps: UploadStepSetup = UploadStepSetup(),
) : UploadContract.View,
    UploadContract.Feedback by dialogs {
    lateinit var activity: UploadActivity
    val binding get() = activity.binding

    init {
        dialogs.view = this
        navigation.view = this
        input.view = this
        steps.view = this
    }

    var isTitleExpanded = true

    var progressDialog: ProgressDialog? = null
    var uploadImagesAdapter: UploadImageAdapter? = null
    var fragments: MutableList<UploadBaseFragment>? = null
    var uploadCategoriesFragment: UploadCategoriesFragment? = null
    var depictsFragment: DepictsFragment? = null
    var mediaLicenseFragment: MediaLicenseFragment? = null
    var thumbnailsAdapter: ThumbnailsAdapter? = null
    var store: BasicKvStore? = null
    var place: Place? = null
    var prevLocation: LatLng? = null
    var currLocation: LatLng? = null
    var isInAppCameraUpload = false

    @JvmField
    var uploadableFiles: MutableList<UploadableFile> = mutableListOf()
    var skippedSharedFilesCount = 0
    var currentSelectedPosition = 0

    /**
     * Returns if multiple files selected or not.
     *
     * Checks for if multiple files selected
     */
    var isMultipleFilesSelected: Boolean = false

    var isShowPermissionsDialog = true

    /**
     * Whether fragments have been saved.
     */
    var isFragmentsSaved = false

    /**
     * Users may uncheck Location tag from the Manage EXIF tags setting any time.
     * So, their location must not be shared in this case.
     *
     */
    val isLocationTagUncheckedInTheSettings: Boolean
        get() {
            val prefExifTags: Set<String> =
                activity.defaultKvStore.getStringSet(Prefs.MANAGED_EXIF_TAGS)
            return !prefExifTags.contains(activity.getString(R.string.exif_tag_location))
        }

    /** Finishes the host when the presenter completes the flow. */
    override fun finish() = activity.finish()

    /** Highlights the image after a cancelled upload. */
    override fun highlightNextImageOnCancelledImage(
        index: Int,
        maxSize: Int,
    ) = navigation.highlightNextImageOnCancelledImage(index, maxSize)

    /** Returns the current selection of uploadable files. */
    override fun getUploadableFiles(): List<UploadableFile> = uploadableFiles

    /** Updates visibility of the thumbnail card. */
    override fun showHideTopCard(shouldShow: Boolean) {
        binding.llContainerTopCard.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    /** Removes the deleted image page and its thumbnail. */
    override fun onUploadMediaDeleted(index: Int) {
        fragments!!.removeAt(index) // Remove the corresponding fragment
        uploadableFiles.removeAt(index) // Remove the files from the list

        val isMediaDetailFragment =
            fragments!!.getOrNull(currentSelectedPosition)?.let {
                it is UploadMediaDetailFragment
            } ?: false
        if (!isMediaDetailFragment) {
            // Should hide the top card current fragment is not the media detail fragment
            showHideTopCard(false)
        }
        thumbnailsAdapter!!.notifyItemRemoved(index) // Notify the thumbnails adapter
        uploadImagesAdapter!!.notifyDataSetChanged() // Notify the ViewPager
    }

    /** Updates the title with the current upload count. */
    override fun updateTopCardTitle() {
        binding.tvTopCardTitle.text =
            activity.resources
                .getQuantityString(
                    R.plurals.upload_count_title,
                    uploadableFiles.size,
                    uploadableFiles.size,
                )
    }

    /** Enqueues the upload worker. */
    override fun makeUploadRequest() {
        makeOneTimeWorkRequest(
            activity.applicationContext,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
        )
    }

    /** Creates the non-cancelable upload progress dialog. */
    fun initProgressDialog() {
        progressDialog = ProgressDialog(activity)
        progressDialog!!.setMessage(activity.getString(R.string.please_wait))
        progressDialog!!.setCancelable(false)
    }
}
