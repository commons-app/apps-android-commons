package fr.free.nrw.commons.contributions

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
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
