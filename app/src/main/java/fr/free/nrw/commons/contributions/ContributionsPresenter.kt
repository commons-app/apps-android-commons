package fr.free.nrw.commons.contributions

import androidx.work.ExistingWorkPolicy
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.di.CommonsApplicationModule
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.worker.WorkRequestHelper.Companion.makeOneTimeWorkRequest
import fr.free.nrw.commons.utils.ImageUtils
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

/**
 * The presenter class for Contributions
 */
class ContributionsPresenter @Inject internal constructor(
    private val contributionsRepository: ContributionsRepository,
    private val uploadRepository: UploadRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioThreadScheduler: Scheduler
) : ContributionsContract.UserActionListener {
    private var compositeDisposable: CompositeDisposable? = null
    private var view: ContributionsContract.View? = null

    @JvmField
    @Inject
    var mediaDataExtractor: MediaDataExtractor? = null

    override fun onAttachView(view: ContributionsContract.View) {
        this.view = view
        compositeDisposable = CompositeDisposable()
    }

    override fun onDetachView() {
        this.view = null
        compositeDisposable!!.clear()
    }

    override fun getContributionsWithTitle(title: String): Contribution {
        return contributionsRepository.getContributionWithFileName(title)
    }

    /**
     * Checks if a contribution is a duplicate and restarts the contribution process if it is not.
     *
     * @param contribution The contribution to check and potentially restart.
     */
    fun checkDuplicateImageAndRestartContribution(contribution: Contribution) {
        compositeDisposable!!.add(
            uploadRepository
                .checkDuplicateImage(contribution.localUriPath!!.path)
                .subscribeOn(ioThreadScheduler)
                .subscribe { imageCheckResult: Int ->
                    if (imageCheckResult == ImageUtils.IMAGE_OK) {
                        contribution.state = Contribution.STATE_QUEUED
                        saveContribution(contribution)
                    } else {
                        Timber.e("Contribution already exists")
                        compositeDisposable!!.add(
                            contributionsRepository
                                .deleteContributionFromDB(contribution)
                                .subscribeOn(ioThreadScheduler)
                                .subscribe()
                        )
                    }
                })
    }

    /**
     * Update the contribution's state in the databse, upon completion, trigger the workmanager to
     * process this contribution
     *
     * @param contribution
     */
    fun saveContribution(contribution: Contribution) {
        compositeDisposable!!.add(contributionsRepository
            .save(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe {
                makeOneTimeWorkRequest(
                    view!!.getContext().applicationContext, ExistingWorkPolicy.KEEP
                )
            })
    }
}
