package fr.free.nrw.commons.upload

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.upload.UploadActivity.Companion.KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE
import fr.free.nrw.commons.upload.UploadActivity.Companion.uploadIsOfAPlace
import fr.free.nrw.commons.upload.categories.UploadCategoriesFragment
import fr.free.nrw.commons.upload.depicts.DepictsFragment
import fr.free.nrw.commons.upload.license.MediaLicenseFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE
import fr.free.nrw.commons.wikidata.WikidataConstants.SELECTED_NEARBY_PLACE_CATEGORY
import java.io.File

/** Creates and restores the fragments for the current upload. */
internal class UploadStepSetup {
    lateinit var view: UploadActivityView
    private val activity get() = view.activity

    /** Formats the supported file types for validation dialogs. */
    fun getSupportedFormatsList(): String {
        val formats =
            listOf(
                activity.getString(R.string.format_jpeg),
                activity.getString(R.string.format_png),
                activity.getString(R.string.format_svg),
                activity.getString(R.string.format_ogg_audio),
            )
        return formats.joinToString(separator = "\n") { "• $it" }
    }

    /** Filters unsupported files and prepares the upload steps. */
    fun receiveSharedItems() {
        val action = activity.intent.action
        if (ContributionController.ACTION_INTERNAL_UPLOADS == action) {
            view.input.receiveInternalSharedItems()
        }

        // Filter out unsupported image types (like HEIC, WebP, GIF) to ensure we strictly accept
        // only JPEG, PNG, and SVG formats, while allowing valid audio files to pass through.
        // Files shared from other apps in such formats were already skipped before being copied.
        val originalSize = view.uploadableFiles.size + view.skippedSharedFilesCount
        view.skippedSharedFilesCount = 0
        view.uploadableFiles =
            view.uploadableFiles
                .filter { file ->
                    file.contentUri?.let { uri ->
                        FileUtils.isSupportedFileType(activity, uri)
                    } ?: false
                }.toMutableList()

        val supportedFormats = getSupportedFormatsList()
        if (view.uploadableFiles.isEmpty() && originalSize > 0) {
            showAlertDialog(
                activity,
                activity.getString(R.string.unsupported_format_title),
                activity.getString(R.string.unsupported_format_desc, supportedFormats),
                activity.getString(R.string.ok),
            ) {
                activity.finish()
            }
            return
        }

        if (view.uploadableFiles.size < originalSize) {
            showAlertDialog(
                activity,
                activity.getString(R.string.unsupported_files_skipped_title),
                activity.getString(R.string.unsupported_files_skipped_desc, supportedFormats),
                activity.getString(R.string.ok),
            ) {
                proceedWithSetup()
            }
            return
        }

        proceedWithSetup()
    }

    /** Prepares thumbnails and pages for the selected files. */
    private fun proceedWithSetup() {
        if (view.uploadableFiles.isEmpty()) {
            view.input.handleNullMedia()
            return
        }

        if (view.uploadableFiles.size > 1) {
            if (!activity.defaultKvStore.getBoolean("hasAlreadyLaunchedCategoriesDialog")) {
                // If there is only file, no need to show the image thumbnails
                view.dialogs.showAlertDialogForCategories()
            }
            if (view.uploadableFiles.size > BATTERY_WARNING_FILE_COUNT &&
                !activity.defaultKvStore.getBoolean("hasAlreadyLaunchedBigMultiupload")
            ) {
                view.dialogs.showAlertForBattery()
            }
            view.thumbnailsAdapter!!.uploadableFiles = view.uploadableFiles
        } else {
            view.binding.llContainerTopCard.visibility = View.GONE
        }
        view.binding.tvTopCardTitle.text =
            activity.resources
                .getQuantityString(
                    R.plurals.upload_count_title,
                    view.uploadableFiles.size,
                    view.uploadableFiles.size,
                )

        if (view.fragments == null) {
            view.fragments = mutableListOf()
        }

        view.uploadableFiles.forEachIndexed { index, file -> addMediaDetails(index, file) }

        // unregister location manager after loop if needed
        if (!uploadIsOfAPlace) {
            activity.locationManager!!.unregisterLocationManager()
        }

        // If fragments are not created, create them and add them to the fragments ArrayList
        if (!view.isFragmentsSaved) {
            addCommonSteps()
        } else {
            for (i in 1 until view.fragments!!.size) {
                view.fragments!![i].callback = createRestoredCallback()
            }
        }

        view.uploadImagesAdapter!!.fragments = view.fragments!!
        view.binding.vpUpload.offscreenPageLimit = view.fragments!!.size

        // Saving size of uploadableFiles
        view.store!!.putInt(KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE, view.uploadableFiles.size)
    }

    /** Creates or reattaches a media details page. */
    private fun addMediaDetails(
        index: Int,
        uploadableFile: UploadableFile,
    ) {
        val uploadMediaDetailFragment = UploadMediaDetailFragment()

        // set fragment properties but defer initialization
        uploadMediaDetailFragment.uploadableFile = uploadableFile
        uploadMediaDetailFragment.place = view.place
        uploadMediaDetailFragment.inAppPictureLocation =
            if (!uploadIsOfAPlace) {
                view.input.handleLocation()
                view.currLocation
            } else {
                view.currLocation
            }

        if (view.uploadImagesAdapter == null) {
            view.navigation.initViewPager()
        }
        val uploadMediaDetailFragmentCallback = createMediaCallback()

        if (view.isFragmentsSaved) {
            val fragment =
                view.fragments!!.getOrNull(index) as? UploadMediaDetailFragment
                    ?: return
            fragment.fragmentCallback = uploadMediaDetailFragmentCallback
            fragment.initializeFragment()
        } else {
            uploadMediaDetailFragment.fragmentCallback = uploadMediaDetailFragmentCallback
            view.fragments!!.add(uploadMediaDetailFragment)
        }
    }

    /** Adds the shared depictions, categories, and license pages. */
    private fun addCommonSteps() {
        view.uploadCategoriesFragment = UploadCategoriesFragment()
        if (view.place != null) {
            val categoryBundle = Bundle()
            categoryBundle.putString(SELECTED_NEARBY_PLACE_CATEGORY, view.place!!.category)
            view.uploadCategoriesFragment!!.arguments = categoryBundle
        }

        view.uploadCategoriesFragment!!.callback = activity

        view.depictsFragment = DepictsFragment()
        val placeBundle = Bundle()
        placeBundle.putParcelable(SELECTED_NEARBY_PLACE, view.place)
        view.depictsFragment!!.arguments = placeBundle
        view.depictsFragment!!.callback = activity

        view.mediaLicenseFragment = MediaLicenseFragment()
        view.mediaLicenseFragment!!.callback = activity

        view.fragments!!.add(view.depictsFragment!!)
        view.fragments!!.add(view.uploadCategoriesFragment!!)
        view.fragments!!.add(view.mediaLicenseFragment!!)
    }

    /** Routes image edits and deletions back to the host. */
    private fun createMediaCallback(): UploadMediaDetailFragmentCallback =
        object : UploadMediaDetailFragmentCallback, UploadBaseFragment.Callback by activity {
            /** Handles the corresponding upload page event. */
            override fun deletePictureAtIndex(index: Int) {
                view.store!!.putInt(
                    KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE,
                    (view.store!!.getInt(KEY_FOR_CURRENT_UPLOAD_IMAGE_SIZE) - 1),
                )
                activity.presenter!!.deletePictureAtIndex(index)
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
            override fun changeThumbnail(
                index: Int,
                uri: String,
            ) {
                view.uploadableFiles.removeAt(index)
                view.uploadableFiles.add(index, UploadableFile(File(uri)))
                view.binding.rvThumbnails.adapter!!
                    .notifyDataSetChanged()
            }
        }

    /** Restores navigation without repeating image validation. */
    private fun createRestoredCallback(): UploadBaseFragment.Callback =
        object : UploadBaseFragment.Callback by activity {
            /** Handles the corresponding upload page event. */
            override fun onNextButtonClicked(index: Int) {
                if (index < view.fragments!!.size - 1) {
                    view.binding.vpUpload.setCurrentItem(index + 1, false)
                    view.fragments!![index + 1].onBecameVisible()
                    (view.binding.rvThumbnails.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            if ((index > 0)) index - 1 else 0,
                            0,
                        )
                } else {
                    activity.presenter!!.handleSubmit()
                }
            }

            /** Handles the corresponding upload page event. */
            override fun onPreviousButtonClicked(index: Int) {
                if (index != 0) {
                    view.binding.vpUpload.setCurrentItem(index - 1, true)
                    view.fragments!![index - 1].onBecameVisible()
                    (view.binding.rvThumbnails.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(
                            if ((index > THUMBNAIL_SCROLL_THRESHOLD)) index - 2 else 0,
                            0,
                        )
                }
            }
        }

    companion object {
        private const val BATTERY_WARNING_FILE_COUNT = 3
        private const val THUMBNAIL_SCROLL_THRESHOLD = 3
    }
}
