package fr.free.nrw.commons.upload

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.work.ExistingWorkPolicy
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_FAILED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_IN_PROGRESS
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_PAUSED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.contributions.ContributionBoundaryCallback
import fr.free.nrw.commons.contributions.ContributionsRemoteDataSource
import fr.free.nrw.commons.contributions.ContributionsRepository
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.Companion.makeOneTimeWorkRequest
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named


/**
 * The presenter class for PendingUploadsFragment and FailedUploadsFragment
 */
class PendingUploadsPresenter @Inject internal constructor(
    private val contributionBoundaryCallback: ContributionBoundaryCallback,
    private val contributionsRemoteDataSource: ContributionsRemoteDataSource,
    private val contributionsRepository: ContributionsRepository,
    private val uploadRepository: UploadRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioThreadScheduler: Scheduler
) : PendingUploadsContract.UserActionListener {
    private val compositeDisposable = CompositeDisposable()

    lateinit var totalContributionList: LiveData<PagedList<Contribution>>
    lateinit var failedContributionList: LiveData<PagedList<Contribution>>

    /**
     * Setups the paged list of Pending Uploads. This method sets the configuration for paged list
     * and ties it up with the live data object. This method can be tweaked to update the lazy
     * loading behavior of the contributions list
     */
    fun setup() {
        val pagedListConfig = PagedList.Config.Builder()
            .setPrefetchDistance(50)
            .setPageSize(10).build()

        val factory = contributionsRepository.fetchContributionsWithStatesSortedByDateUploadStarted(
            listOf(STATE_QUEUED, STATE_IN_PROGRESS, STATE_PAUSED)
        )
        totalContributionList = LivePagedListBuilder(factory, pagedListConfig).build()
    }

    /**
     * Setups the paged list of Failed Uploads. This method sets the configuration for paged list
     * and ties it up with the live data object. This method can be tweaked to update the lazy
     * loading behavior of the contributions list
     */
    fun getFailedContributions() {
        val pagedListConfig = PagedList.Config.Builder()
            .setPrefetchDistance(50)
            .setPageSize(10).build()

        val factory = contributionsRepository.fetchContributionsWithStatesSortedByDateUploadStarted(
            listOf(STATE_FAILED)
        )
        failedContributionList = LivePagedListBuilder(factory, pagedListConfig).build()
    }

    override fun onAttachView(view: PendingUploadsContract.View) {
    }

    override fun onDetachView() {
        compositeDisposable.clear()
        contributionsRemoteDataSource.dispose()
        contributionBoundaryCallback.dispose()
    }

    /**
     * Deletes the specified upload (contribution) from the database.
     *
     * @param contribution The contribution object representing the upload to be deleted.
     * @param context      The context in which the operation is being performed.
     */
    override fun deleteUpload(contribution: Contribution?, context: Context?) {
        compositeDisposable.add(
            contributionsRepository
                .deleteContributionFromDB(contribution)
                .subscribeOn(ioThreadScheduler)
                .subscribe()
        )
    }

    /**
     * Pauses all the uploads by changing the state of contributions from STATE_QUEUED and
     * STATE_IN_PROGRESS to STATE_PAUSED in the database.
     */
    fun pauseUploads() {
        CommonsApplication.isPaused = true
        compositeDisposable.add(
            contributionsRepository
                .updateContributionsWithStates(
                    listOf(STATE_QUEUED, STATE_IN_PROGRESS),
                    STATE_PAUSED
                )
                .subscribeOn(ioThreadScheduler)
                .subscribe()
        )
    }

    /**
     * Deletes contributions from the database that match the specified states.
     *
     * @param states A list of integers representing the states of the contributions to be deleted.
     */
    fun deleteUploads(states: List<Int>) {
        compositeDisposable.add(
            contributionsRepository
                .deleteContributionsFromDBWithStates(states)
                .subscribeOn(ioThreadScheduler)
                .subscribe()
        )
    }

    /**
     * Restarts the uploads for the specified list of contributions starting from the given index.
     *
     * @param contributionList The list of contributions to be restarted.
     * @param index            The starting index in the list from which to restart uploads.
     * @param context          The context in which the operation is being performed.
     */
    fun restartUploads(contributionList: List<Contribution>, index: Int, context: Context) {
        CommonsApplication.isPaused = false
        if (index >= contributionList.size) {
            return
        }
        val contribution = contributionList[index]
        if (contribution.state == STATE_FAILED) {
            contribution.dateUploadStarted = Calendar.getInstance().time
            if (contribution.errorInfo == null) {
                contribution.chunkInfo = null
                contribution.transferred = 0
            }
            compositeDisposable.add(
                uploadRepository
                    .checkDuplicateImage(
                        originalFilePath = contribution.contentUri!!,
                        modifiedFilePath = contribution.localUri!!
                    )
                    .subscribeOn(ioThreadScheduler)
                    .subscribe({ imageCheckResult: Int ->
                        if (imageCheckResult == IMAGE_OK) {
                            contribution.state = STATE_QUEUED
                            compositeDisposable.add(
                                contributionsRepository
                                    .save(contribution)
                                    .subscribeOn(ioThreadScheduler)
                                    .doOnComplete {
                                        restartUploads(contributionList, index + 1, context)
                                    }
                                    .subscribe {
                                        makeOneTimeWorkRequest(
                                            context, ExistingWorkPolicy.KEEP
                                        )
                                    })
                        } else {
                            Timber.e("Contribution already exists")
                            compositeDisposable.add(
                                contributionsRepository
                                    .deleteContributionFromDB(contribution)
                                    .subscribeOn(ioThreadScheduler).doOnComplete {
                                        restartUploads(contributionList, index + 1, context)
                                    }
                                    .subscribe())
                        }
                    }, { throwable: Throwable? ->
                        Timber.e(throwable)
                        restartUploads(contributionList, index + 1, context)
                    })
            )
        } else {
            contribution.state = STATE_QUEUED
            compositeDisposable.add(
                contributionsRepository
                    .save(contribution)
                    .subscribeOn(ioThreadScheduler)
                    .doOnComplete {
                        restartUploads(contributionList, index + 1, context)
                    }
                    .subscribe {
                        makeOneTimeWorkRequest(context, ExistingWorkPolicy.KEEP)
                    }
            )
        }
    }

    /**
     * Restarts the upload for the specified list of contributions for the given index.
     *
     * @param contributionList The list of contributions.
     * @param index            The index in the list which to be restarted.
     * @param context          The context in which the operation is being performed.
     */
    fun restartUpload(contributionList: List<Contribution>, index: Int, context: Context) {
        CommonsApplication.isPaused = false
        if (index >= contributionList.size) {
            return
        }
        val contribution = contributionList[index]
        if (contribution.state == STATE_FAILED) {
            contribution.dateUploadStarted = Calendar.getInstance().time
            if (contribution.errorInfo == null) {
                contribution.chunkInfo = null
                contribution.transferred = 0
            }
            compositeDisposable.add(
                uploadRepository
                    .checkDuplicateImage(
                        originalFilePath = contribution.contentUri!!,
                        modifiedFilePath = contribution.localUri!!
                    )
                    .subscribeOn(ioThreadScheduler)
                    .subscribe { imageCheckResult: Int ->
                        if (imageCheckResult == IMAGE_OK) {
                            contribution.state = STATE_QUEUED
                            compositeDisposable.add(
                                contributionsRepository
                                    .save(contribution)
                                    .subscribeOn(ioThreadScheduler)
                                    .subscribe {
                                        makeOneTimeWorkRequest(context, ExistingWorkPolicy.KEEP)
                                    }
                            )
                        } else {
                            Timber.e("Contribution already exists")
                            compositeDisposable.add(
                                contributionsRepository
                                    .deleteContributionFromDB(contribution)
                                    .subscribeOn(ioThreadScheduler)
                                    .subscribe()
                            )
                        }
                    })
        } else {
            contribution.state = STATE_QUEUED
            compositeDisposable.add(
                contributionsRepository
                    .save(contribution)
                    .subscribeOn(ioThreadScheduler)
                    .subscribe {
                        makeOneTimeWorkRequest(context, ExistingWorkPolicy.KEEP)
                    }
            )
        }
    }
}
