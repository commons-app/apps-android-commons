package fr.free.nrw.commons.media

import android.content.SharedPreferences
import android.widget.Toast
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.OnSwipeTouchListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.launch

/**
 * Handles swipe gestures on [ZoomableActivity], including folder navigation and
 * select / not-for-upload actions.
 *
 * @param activity Host activity whose state and views are updated by swipe actions.
 */
class ZoomableActivitySwipeHandler(
    private val activity: ZoomableActivity,
) {
    /**
     * Handle swap gestures. Ex. onSwipeLeft, onSwipeRight, onSwipeUp, onSwipeDown
     */
    fun onSwipe() {
        val sharedPreferences: SharedPreferences =
            activity.getSharedPreferences(ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val showAlreadyActionedImages =
            sharedPreferences.getBoolean(ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        if (!activity.images.isNullOrEmpty()) {
            activity.binding.zoomable.setOnTouchListener(
                object : OnSwipeTouchListener(activity) {
                    // Swipe left to view next image in the folder. (if available)
                    override fun onSwipeLeft() {
                        super.onSwipeLeft()
                        onLeftSwiped(showAlreadyActionedImages)
                    }

                    // Swipe right to view previous image in the folder. (if available)
                    override fun onSwipeRight() {
                        super.onSwipeRight()
                        onRightSwiped(showAlreadyActionedImages)
                    }

                    // Swipe up to select the picture (the equivalent of tapping it in non-fullscreen mode)
                    // and show the next picture skipping pictures that have either already been uploaded or
                    // marked as not for upload
                    override fun onSwipeUp() {
                        super.onSwipeUp()
                        onUpSwiped()
                    }

                    // Swipe down to mark that picture as "Not for upload" (the equivalent of selecting it then
                    // tapping "Mark as not for upload" in non-fullscreen mode), and show the next picture.
                    override fun onSwipeDown() {
                        super.onSwipeDown()
                        onDownSwiped()
                    }
                },
            )
        }
    }

    /**
     * Handles down swipe action
     */
    fun onDownSwiped() {
        if (!activity.binding.zoomable
                .getZoomableController()
                .isIdentity()
        ) {
            return
        }

        activity.scope.launch {
            val imageSHA1 =
                CustomSelectorUtils.getImageSHA1(
                    activity.images!![activity.position].uri,
                    activity.ioDispatcher,
                    activity.fileUtilsWrapper,
                    activity.contentResolver,
                )
            var isUploaded = activity.uploadedStatusDao.findByImageSHA1(imageSHA1, true)
            if (isUploaded > 0) {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.this_image_is_already_uploaded),
                        Toast.LENGTH_SHORT,
                    ).show()
            } else {
                val imageModifiedSHA1 =
                    CustomSelectorUtils.generateModifiedSHA1(
                        activity.images!![activity.position],
                        activity.defaultDispatcher,
                        activity,
                        activity.fileProcessor,
                        activity.fileUtilsWrapper,
                    )
                isUploaded =
                    activity.uploadedStatusDao.findByModifiedImageSHA1(
                        imageModifiedSHA1,
                        true,
                    )
                if (isUploaded > 0) {
                    Toast
                        .makeText(
                            activity,
                            activity.getString(R.string.this_image_is_already_uploaded),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    insertInNotForUpload(activity.images!![activity.position])
                    Toast
                        .makeText(
                            activity,
                            activity.getString(R.string.image_marked_as_not_for_upload),
                            Toast.LENGTH_SHORT,
                        ).show()
                    activity.shouldRefresh = true
                    if (activity.position < activity.images!!.size - 1) {
                        activity.position++
                        activity.init(activity.images!![activity.position].uri)
                    } else {
                        Toast
                            .makeText(
                                activity,
                                activity.getString(R.string.no_more_images_found),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        }
    }

    /**
     * Handles up swipe action
     */
    fun onUpSwiped() {
        if (!activity.binding.zoomable
                .getZoomableController()
                .isIdentity()
        ) {
            return
        }

        activity.scope.launch {
            val imageSHA1 =
                CustomSelectorUtils.getImageSHA1(
                    activity.images!![activity.position].uri,
                    activity.ioDispatcher,
                    activity.fileUtilsWrapper,
                    activity.contentResolver,
                )
            var isNonActionable = activity.notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable > 0) {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.can_not_select_this_image_for_upload),
                        Toast.LENGTH_SHORT,
                    ).show()
            } else {
                isNonActionable =
                    activity.uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable > 0) {
                    Toast
                        .makeText(
                            activity,
                            activity.getString(R.string.this_image_is_already_uploaded),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    val imageModifiedSHA1 =
                        CustomSelectorUtils.generateModifiedSHA1(
                            activity.images!![activity.position],
                            activity.defaultDispatcher,
                            activity,
                            activity.fileProcessor,
                            activity.fileUtilsWrapper,
                        )
                    isNonActionable =
                        activity.uploadedStatusDao.findByModifiedImageSHA1(
                            imageModifiedSHA1,
                            true,
                        )
                    if (isNonActionable > 0) {
                        Toast
                            .makeText(
                                activity,
                                activity.getString(R.string.this_image_is_already_uploaded),
                                Toast.LENGTH_SHORT,
                            ).show()
                    } else {
                        if (!activity.selectedImages!!.contains(activity.images!![activity.position])) {
                            activity.selectedImages!!.add(activity.images!![activity.position])
                            Toast
                                .makeText(
                                    activity,
                                    activity.getString(R.string.image_selected),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                        activity.position = getNextActionableImage(activity.position + 1)
                        activity.init(activity.images!![activity.position].uri)
                    }
                }
            }
        }
    }

    /**
     * Handles right swipe action
     */
    fun onRightSwiped(showAlreadyActionedImages: Boolean) {
        if (!activity.binding.zoomable
                .getZoomableController()
                .isIdentity()
        ) {
            return
        }

        if (showAlreadyActionedImages) {
            if (activity.position > 0) {
                activity.position--
                activity.init(activity.images!![activity.position].uri)
            } else {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.no_more_images_found),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } else {
            if (activity.position > 0) {
                activity.scope.launch {
                    activity.position = getPreviousActionableImage(activity.position - 1)
                    activity.init(activity.images!![activity.position].uri)
                }
            } else {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.no_more_images_found),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    /**
     * Handles left swipe action
     */
    fun onLeftSwiped(showAlreadyActionedImages: Boolean) {
        if (!activity.binding.zoomable
                .getZoomableController()
                .isIdentity()
        ) {
            return
        }

        if (showAlreadyActionedImages) {
            if (activity.position < activity.images!!.size - 1) {
                activity.position++
                activity.init(activity.images!![activity.position].uri)
            } else {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.no_more_images_found),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } else {
            if (activity.position < activity.images!!.size - 1) {
                activity.scope.launch {
                    activity.position = getNextActionableImage(activity.position + 1)
                    activity.init(activity.images!![activity.position].uri)
                }
            } else {
                Toast
                    .makeText(
                        activity,
                        activity.getString(R.string.no_more_images_found),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    /**
     * Gets next actionable image.
     * Iterates from an index to the end of the folder and check whether the current image is
     * present in already uploaded table or in not for upload table,
     * and returns the first actionable image it can find.
     */
    suspend fun getNextActionableImage(index: Int): Int {
        var nextPosition = activity.position
        for (i in index until activity.images!!.size) {
            nextPosition = i
            val imageSHA1 =
                CustomSelectorUtils.getImageSHA1(
                    activity.images!![i].uri,
                    activity.ioDispatcher,
                    activity.fileUtilsWrapper,
                    activity.contentResolver,
                )
            var isNonActionable = activity.notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable <= 0) {
                isNonActionable = activity.uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable <= 0) {
                    val imageModifiedSHA1 =
                        CustomSelectorUtils.generateModifiedSHA1(
                            activity.images!![i],
                            activity.defaultDispatcher,
                            activity,
                            activity.fileProcessor,
                            activity.fileUtilsWrapper,
                        )
                    isNonActionable =
                        activity.uploadedStatusDao.findByModifiedImageSHA1(
                            imageModifiedSHA1,
                            true,
                        )
                    if (isNonActionable <= 0) {
                        return i
                    } else {
                        continue
                    }
                } else {
                    continue
                }
            } else {
                continue
            }
        }
        return nextPosition
    }

    /**
     * Gets previous actionable image.
     * Iterates from an index to the first image of the folder and check whether the current image
     * is present in already uploaded table or in not for upload table,
     * and returns the first actionable image it can find
     */
    suspend fun getPreviousActionableImage(index: Int): Int {
        var previousPosition = activity.position
        for (i in index downTo 0) {
            previousPosition = i
            val imageSHA1 =
                CustomSelectorUtils.getImageSHA1(
                    activity.images!![i].uri,
                    activity.ioDispatcher,
                    activity.fileUtilsWrapper,
                    activity.contentResolver,
                )
            var isNonActionable = activity.notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable <= 0) {
                isNonActionable = activity.uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable <= 0) {
                    val imageModifiedSHA1 =
                        CustomSelectorUtils.generateModifiedSHA1(
                            activity.images!![i],
                            activity.defaultDispatcher,
                            activity,
                            activity.fileProcessor,
                            activity.fileUtilsWrapper,
                        )
                    isNonActionable =
                        activity.uploadedStatusDao.findByModifiedImageSHA1(
                            imageModifiedSHA1,
                            true,
                        )
                    if (isNonActionable <= 0) {
                        return i
                    } else {
                        continue
                    }
                } else {
                    continue
                }
            } else {
                continue
            }
        }
        return previousPosition
    }

    /**
     * Inserts an image in Not For Upload table
     */
    suspend fun insertInNotForUpload(it: Image) {
        val imageSHA1 =
            CustomSelectorUtils.getImageSHA1(
                it.uri,
                activity.ioDispatcher,
                activity.fileUtilsWrapper,
                activity.contentResolver,
            )
        activity.notForUploadStatusDao.insert(
            NotForUploadStatus(
                imageSHA1,
            ),
        )
    }
}
