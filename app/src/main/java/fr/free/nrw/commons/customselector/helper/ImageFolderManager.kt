package fr.free.nrw.commons.customselector.helper

import android.content.Context
import android.content.SharedPreferences
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.helper.ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.helper.ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import fr.free.nrw.commons.customselector.ui.selector.ImageFragment
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.TreeMap
import javax.inject.Inject

/**
 * Helper class that manages folder-level operations, contribution queries,
 * switch state persistence, and scroll position tracking for custom selector images.
 */
class ImageFolderManager
    @Inject
    constructor(
        private val contributionDao: ContributionDao,
    ) {
        /**
         * Get uploading contributions from database.
         */
        fun getUploadingContributions(): List<Contribution> =
            contributionDao
                .getContribution(
                    listOf(
                        Contribution.STATE_IN_PROGRESS,
                        Contribution.STATE_FAILED,
                        Contribution.STATE_QUEUED,
                        Contribution.STATE_PAUSED,
                    ),
                )?.subscribeOn(Schedulers.io())
                ?.blockingGet() ?: emptyList()

        /**
         * Handles changes in the switch state to show or hide already actioned images.
         */
        fun onSwitchChanged(
            checked: Boolean,
            allImages: ArrayList<Image>,
            imageAdapter: ImageAdapter,
            context: Context,
        ) {
            ImageFragment.showAlreadyActionedImages = checked
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences(CUSTOM_SELECTOR_PREFERENCE_KEY, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean(SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, checked)
            editor.apply()

            val uploadingContributions = getUploadingContributions()
            // init() internally uses DiffUtil and dispatches its own updates;
            // a redundant notifyDataSetChanged() here would trigger a second full
            // rebind of every visible ViewHolder, causing a scroll-performance regression.
            imageAdapter.init(allImages, allImages, TreeMap(), uploadingContributions)
        }

        /**
         * Filter Image List based on bucketId.
         */
        fun filterImages(
            images: ArrayList<Image>,
            bucketId: Long?,
        ): ArrayList<Image> = ImageHelper.filterImages(images, bucketId)

        /**
         * Processes result images for a folder, returning filtered images.
         */
        fun processResultImages(
            images: ArrayList<Image>,
            bucketId: Long?,
            imageAdapter: ImageAdapter,
            selectedImages: ArrayList<Image>?,
        ): ArrayList<Image> {
            val uploadingContributions = getUploadingContributions()
            val filteredImages = filterImages(images, bucketId)
            val allImages = ArrayList(filteredImages)
            // init() uses DiffUtil internally; the caller (ImageFragment's observer)
            // issues its own notifyDataSetChanged() after this returns, so a second
            // call here would cause a double full-rebind and hurt scroll performance.
            imageAdapter.init(filteredImages, allImages, TreeMap(), uploadingContributions)
            selectedImages?.let {
                imageAdapter.selectionManager.setSelectedImages(it)
            }
            return allImages
        }

        /**
         * Saves the last visible item ID to shared preferences.
         */
        fun saveLastVisibleItemId(
            position: Int,
            filteredImages: List<Image>,
            imageAdapter: ImageAdapter?,
            context: Context?,
        ) {
            if (position == -1 || filteredImages.isEmpty() || imageAdapter == null) {
                Timber.d(
                    "Skipped saving item ID: position=%d, filteredImages.size=%d, imageAdapter initialized=%b",
                    position,
                    filteredImages.size,
                    imageAdapter != null,
                )
                return
            }

            val prefs = context?.getSharedPreferences("CustomSelector", Context.MODE_PRIVATE) ?: return
            val imageId = imageAdapter.getImageIdAt(position)
            prefs.edit().putLong("ItemId", imageId).apply()
            Timber.d("Saved last visible item ID: %d", imageId)
        }

        /**
         * Cleans up the image adapter.
         */
        fun cleanup(imageAdapter: ImageAdapter?) {
            imageAdapter?.let {
                it.cleanUp()
                Timber.d("Cleaned up imageAdapter in onDestroy")
            } ?: run {
                Timber.w("imageAdapter not initialized in onDestroy, skipping cleanup")
            }
        }
    }
