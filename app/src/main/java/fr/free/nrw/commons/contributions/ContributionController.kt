package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.MAX_IMAGE_COUNT
import fr.free.nrw.commons.filepicker.DefaultCallback
import fr.free.nrw.commons.filepicker.ExternalImagesResult
import fr.free.nrw.commons.filepicker.FilePicker.handleExternalImagesPicked
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContributionController
    @Inject
    constructor() {
        // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
        // LiveData<PagedList<Contribution>> failedAndPendingContributionList;
        @JvmField
        var pendingContributionList: LiveData<PagedList<Contribution>>? = null

        @JvmField
        var failedContributionList: LiveData<PagedList<Contribution>>? = null

        @JvmField
        @Inject
        var repository: ContributionsRepository? = null

        fun handleExternalImagesPicked(
            activity: Activity,
            data: Intent?,
        ): ExternalImagesResult = handleExternalImagesPicked(data, activity)

        /**
         * Returns intent to be passed to upload activity Attaches place object for nearby uploads and
         * location before image capture if in-app camera is used
         */
        private fun handleImagesPicked(
            context: Context,
            imagesFiles: List<UploadableFile>,
        ): Intent {
            // enforce the limit here for the internal gallery picks
            val filesToUpload =
                if (imagesFiles.size > MAX_IMAGE_COUNT) {
                    showLongToast(context, context.getString(R.string.upload_limit_exceeded, MAX_IMAGE_COUNT))
                    imagesFiles.take(MAX_IMAGE_COUNT)
                } else {
                    imagesFiles
                }

            val shareIntent = Intent(context, UploadActivity::class.java)
            shareIntent.setAction(ACTION_INTERNAL_UPLOADS)
            shareIntent
                .putParcelableArrayListExtra(UploadActivity.EXTRA_FILES, ArrayList(filesToUpload))
            val place = defaultKvStore.getJson<Place>(PLACE_OBJECT, Place::class.java)

            if (place != null) {
                shareIntent.putExtra(PLACE_OBJECT, place)
            }

            if (locationBeforeImageCapture != null) {
                shareIntent.putExtra(
                    UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE,
                    locationBeforeImageCapture,
                )
            }

            shareIntent.putExtra(
                UploadActivity.IN_APP_CAMERA_UPLOAD,
                isInAppCameraUpload,
            )
            isInAppCameraUpload = false // reset the flag for next use
            return shareIntent
        }

        val pendingContributions: Unit
            /**
             * Fetches the contributions with the state "IN_PROGRESS", "QUEUED" and "PAUSED" and then it
             * populates the `pendingContributionList`.
             */
            get() {
                val pagedListConfig =
                    (PagedList.Config.Builder())
                        .setPrefetchDistance(50)
                        .setPageSize(10)
                        .build()
                val factory =
                    repository!!.fetchContributionsWithStates(
                        Arrays.asList(
                            Contribution.STATE_IN_PROGRESS,
                            Contribution.STATE_QUEUED,
                            Contribution.STATE_PAUSED,
                        ),
                    )

                val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)
                pendingContributionList = livePagedListBuilder.build()
            }

        val failedContributions: Unit
            /**
             * Fetches the contributions with the state "FAILED" and populates the
             * `failedContributionList`.
             */
            get() {
                val pagedListConfig =
                    (PagedList.Config.Builder())
                        .setPrefetchDistance(50)
                        .setPageSize(10)
                        .build()
                val factory =
                    repository!!.fetchContributionsWithStates(
                        listOf(Contribution.STATE_FAILED),
                    )

                val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)
                failedContributionList = livePagedListBuilder.build()
            }

        // Temporarily disabled, see issue [https://github.com/commons-app/apps-android-commons/issues/5847]
        // Fetches the contributions with the state "IN_PROGRESS", "QUEUED", "PAUSED" and "FAILED" and
        // then it populates the `failedAndPendingContributionList`.
        //
        //    void getFailedAndPendingContributions() {
        //        final PagedList.Config pagedListConfig =
        //            (new PagedList.Config.Builder())
        //                .setPrefetchDistance(50)
        //                .setPageSize(10).build();
        //        Factory<Integer, Contribution> factory;
        //        factory = repository.fetchContributionsWithStates(
        //            Arrays.asList(Contribution.STATE_IN_PROGRESS, Contribution.STATE_QUEUED,
        //                Contribution.STATE_PAUSED, Contribution.STATE_FAILED));
        //
        //        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
        //            pagedListConfig);
        //        failedAndPendingContributionList = livePagedListBuilder.build();
        //    }

        companion object {
            const val ACTION_INTERNAL_UPLOADS: String = "internalImageUploads"
        }
    }
