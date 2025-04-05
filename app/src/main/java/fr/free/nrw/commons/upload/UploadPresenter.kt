package fr.free.nrw.commons.upload

import android.annotation.SuppressLint
import fr.free.nrw.commons.CommonsApplication.Companion.IS_LIMITED_CONNECTION_MODE_ENABLED
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract
import io.reactivex.Observer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
class UploadPresenter @Inject internal constructor(
    private val repository: UploadRepository,
    @param:Named("default_preferences") private val defaultKvStore: JsonKvStore
) : UploadContract.UserActionListener {
    private var view = DUMMY

    @Inject
    lateinit var presenter: UploadMediaDetailsContract.UserActionListener

    private val compositeDisposable = CompositeDisposable()

    lateinit var basicKvStoreFactory: (String) -> BasicKvStore

    /**
     * Called by the submit button in [UploadActivity]
     */
    @SuppressLint("CheckResult")
    override fun handleSubmit() {
        var hasLocationProvidedForNewUploads = false
        for (item in repository.getUploads()) {
            if (item.gpsCoords?.imageCoordsExists == true) {
                hasLocationProvidedForNewUploads = true
            }
        }
        val hasManyConsecutiveUploadsWithoutLocation = defaultKvStore.getInt(
            COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0
        ) >=
                CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD

        if (hasManyConsecutiveUploadsWithoutLocation && !hasLocationProvidedForNewUploads) {
            defaultKvStore.putInt(COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0)
            view.showAlertDialog(
                R.string.location_message
            ) {
                defaultKvStore.putInt(
                    COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES,
                    0
                )
                processContributionsForSubmission()
            }
        } else {
            processContributionsForSubmission()
        }
    }

    private fun processContributionsForSubmission() {
        if (view.isLoggedIn()) {
            view.showProgress(true)
            repository.buildContributions().observeOn(Schedulers.io())
                ?.subscribe(object : Observer<Contribution> {
                    override fun onSubscribe(d: Disposable) {
                        view.showProgress(false)
                        if (defaultKvStore.getBoolean(IS_LIMITED_CONNECTION_MODE_ENABLED, false)) {
                            view.showMessage(R.string.uploading_queued)
                        } else {
                            view.showMessage(R.string.uploading_started)
                        }
                        compositeDisposable.add(d)
                    }

                    override fun onNext(contribution: Contribution) {
                        if (contribution.decimalCoords == null) {
                            val recentCount = defaultKvStore.getInt(
                                COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0
                            )
                            defaultKvStore.putInt(
                                COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, recentCount + 1
                            )
                        } else {
                            defaultKvStore.putInt(
                                COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES, 0
                            )
                        }
                        repository.prepareMedia(contribution)
                        contribution.state = Contribution.STATE_QUEUED
                        repository.saveContribution(contribution)
                    }

                    override fun onError(e: Throwable) {
                        view.showMessage(R.string.upload_failed)
                        repository.cleanup()
                        view.returnToMainActivity()
                        compositeDisposable.clear()
                        Timber.e(e, "failed to upload")

                        //is submission error, not need to go to the uploadActivity
                        //not start the uploading progress
                    }

                    override fun onComplete() {
                        view.makeUploadRequest()
                        repository.cleanup()
                        view.returnToMainActivity()
                        compositeDisposable.clear()

                        //after finish the uploadActivity, if successful,
                        //directly go to the upload progress activity
                        view.goToUploadProgressActivity()
                    }
                })
        } else {
            view.askUserToLogIn()
        }
    }

    override fun setupBasicKvStoreFactory(factory: (String) -> BasicKvStore) {
        basicKvStoreFactory = factory
    }

    /**
     * Calls checkImageQuality of UploadMediaPresenter to check image quality of next image
     *
     * @param uploadItemIndex Index of next image, whose quality is to be checked
     */
    override fun checkImageQuality(uploadItemIndex: Int) {
        repository.getUploadItem(uploadItemIndex)?.let {
            presenter.setupBasicKvStoreFactory(basicKvStoreFactory)
            presenter.checkImageQuality(it, uploadItemIndex)
        }
    }

    override fun deletePictureAtIndex(index: Int) {
        val uploadableFiles = view.getUploadableFiles()
        uploadableFiles?.let {
            view.setImageCancelled(true)
            repository.deletePicture(uploadableFiles[index].getFilePath())
            if (uploadableFiles.size == 1) {
                view.showMessage(R.string.upload_cancelled)
                view.finish()
                return
            }

            presenter.updateImageQualitiesJSON(uploadableFiles.size, index)
            view.onUploadMediaDeleted(index)
            if (index != uploadableFiles.size && index != 0) {
                // if the deleted image was not the last item to be uploaded, check quality of next
                repository.getUploadItem(index)?.let {
                    presenter.checkImageQuality(it, index)
                }
            }

            if (uploadableFiles.size < 2) {
                view.showHideTopCard(false)
            }

            //In case lets update the number of uploadable media
            view.updateTopCardTitle()
        }
    }

    override fun onAttachView(view: UploadContract.View) {
        this.view = view
    }

    override fun onDetachView() {
        view = DUMMY
        compositeDisposable.clear()
        repository.cleanup()
    }

    companion object {
        private val DUMMY = Proxy.newProxyInstance(
            UploadContract.View::class.java.classLoader,
            arrayOf<Class<*>>(UploadContract.View::class.java)
        ) { _: Any?, _: Method?, _: Array<Any?>? -> null } as UploadContract.View

        const val COUNTER_OF_CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES: String =
            "number_of_consecutive_uploads_without_coordinates"

        const val CONSECUTIVE_UPLOADS_WITHOUT_COORDINATES_REMINDER_THRESHOLD: Int = 10
    }
}
