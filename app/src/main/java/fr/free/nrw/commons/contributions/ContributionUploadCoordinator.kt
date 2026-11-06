package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.DefaultCallback
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.filepicker.FilePicker.HandleActivityResult
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.UploadActivity
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Owns file-picker result handling and the handoff from selected files to upload activity.
 */
@Singleton
class ContributionUploadCoordinator
    @Inject
    constructor(
        @param:Named("default_preferences") private val defaultKvStore: JsonKvStore,
        private val cameraCoordinator: ContributionCameraCoordinator,
    ) {
        var isInternalUploadInProgress = false

        /**
         * Attaches callbacks that handle picker cancellation, errors, and successful upload handoff.
         *
         * @param activity activity starting the upload activity
         * @param handleActivityResult file-picker result handler
         */
        fun handleActivityResultWithCallback(
            activity: Activity,
            handleActivityResult: HandleActivityResult,
        ) {
            handleActivityResult.onHandleActivityResult(
                object : DefaultCallback() {
                    /**
                     * Resets upload state and removes the stored nearby place after cancellation.
                     */
                    override fun onCanceled(
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        super.onCanceled(source, type)
                        isInternalUploadInProgress = false
                        defaultKvStore.remove(PLACE_OBJECT)
                    }

                    /**
                     * Shows an error toast when the image picker reports an error.
                     */
                    override fun onImagePickerError(
                        e: Exception,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        showShortToast(activity, R.string.error_occurred_in_picking_images)
                    }

                    /**
                     * Creates the upload intent and starts the upload activity.
                     */
                    override fun onImagesPicked(
                        imagesFiles: List<UploadableFile>,
                        source: FilePicker.ImageSource,
                        type: Int,
                    ) {
                        val cameraMetadata = cameraCoordinator.takeUploadMetadata()
                        val shareIntent = Intent(activity, UploadActivity::class.java)
                        shareIntent.setAction(ContributionController.ACTION_INTERNAL_UPLOADS)
                        shareIntent
                            .putParcelableArrayListExtra(
                                UploadActivity.EXTRA_FILES,
                                ArrayList(imagesFiles),
                            )
                        val place = defaultKvStore.getJson<Place>(PLACE_OBJECT, Place::class.java)

                        if (place != null) {
                            shareIntent.putExtra(PLACE_OBJECT, place)
                        }

                        if (cameraMetadata.locationBeforeImageCapture != null) {
                            shareIntent.putExtra(
                                UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE,
                                cameraMetadata.locationBeforeImageCapture,
                            )
                        }

                        shareIntent.putExtra(
                            UploadActivity.IN_APP_CAMERA_UPLOAD,
                            cameraMetadata.isInAppCameraUpload,
                        )
                        activity.startActivity(shareIntent)
                    }
                },
            )
        }
    }
