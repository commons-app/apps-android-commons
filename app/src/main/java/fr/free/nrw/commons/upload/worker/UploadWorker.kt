package fr.free.nrw.commons.upload.worker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mapbox.mapboxsdk.plugins.localization.BuildConfig
import dagger.android.ContributesAndroidInjector
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.ChunkInfo
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.upload.StashUploadState
import fr.free.nrw.commons.upload.UploadClient
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.wikidata.WikidataEditService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.collections.ArrayList

class UploadWorker(var appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private var notificationManager: NotificationManagerCompat? = null

    @Inject
    lateinit var wikidataEditService: WikidataEditService

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var contributionDao: ContributionDao

    @Inject
    lateinit var uploadClient: UploadClient

    @Inject
    lateinit var mediaClient: MediaClient

    private val PROCESSING_UPLOADS_NOTIFICATION_TAG = BuildConfig.APPLICATION_ID + " : upload_tag"

    private val PROCESSING_UPLOADS_NOTIFICATION_ID = 101


    //Attributes of the current-upload notification
    private var currentNotificationID: Int = -1// lateinit is not allowed with primitives
    private lateinit var currentNotificationTag: String
    private var curentNotification: NotificationCompat.Builder

    private val statesToProcess= ArrayList<Int>()

    private val STASH_ERROR_CODES = Arrays
        .asList(
            "uploadstash-file-not-found",
            "stashfailed",
            "verification-error",
            "chunk-too-small"
        )

    init {
        ApplicationlessInjection
            .getInstance(appContext)
            .commonsApplicationComponent
            .inject(this)
        curentNotification =
            getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)!!

        statesToProcess.add(Contribution.STATE_QUEUED)
        statesToProcess.add(Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE)
    }

    @dagger.Module
    interface Module {
        @ContributesAndroidInjector
        fun worker(): UploadWorker
    }

    open inner class NotificationUpdateProgressListener(
        private var notificationFinishingTitle: String?,
        var contribution: Contribution?
    ) {

        fun onProgress(transferred: Long, total: Long) {
            if (transferred == total) {
                // Completed!
                curentNotification.setContentTitle(notificationFinishingTitle)
                    .setProgress(0, 100, true)
            } else {
                curentNotification
                    .setProgress(
                        100,
                        (transferred.toDouble() / total.toDouble() * 100).toInt(),
                        false
                    )
            }
            notificationManager?.notify(
                currentNotificationTag,
                currentNotificationID,
                curentNotification.build()!!
            )
            contribution!!.transferred = transferred
            contributionDao.update(contribution).blockingAwait()
        }

        open fun onChunkUploaded(contribution: Contribution, chunkInfo: ChunkInfo?) {
            contribution.chunkInfo = chunkInfo
            contributionDao.update(contribution).blockingAwait()
        }
    }

    private fun getNotificationBuilder(channelId: String): NotificationCompat.Builder? {
        return NotificationCompat.Builder(appContext, channelId)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    appContext.resources,
                    R.drawable.ic_launcher
                )
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            .setOngoing(true)
    }

    override suspend fun doWork(): Result {
        notificationManager = NotificationManagerCompat.from(appContext)
        val processingUploads = getNotificationBuilder(
            CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL
        )!!
        withContext(Dispatchers.IO) {
            //Doing this so that retry requests do not create new work requests and while a work is
            // already running, all the requests should go through this, so kind of a queue
            while (contributionDao.getContribution(statesToProcess)
                    .blockingGet().isNotEmpty()
            ) {
                val queuedContributions = contributionDao.getContribution(statesToProcess)
                    .blockingGet()
                //Showing initial notification for the number of uploads being processed

                processingUploads.setContentTitle(appContext.getString(R.string.starting_uploads))
                processingUploads.setContentText(
                    appContext.resources.getQuantityString(
                        R.plurals.starting_multiple_uploads,
                        queuedContributions.size,
                        queuedContributions.size
                    )
                )
                notificationManager?.notify(
                    PROCESSING_UPLOADS_NOTIFICATION_TAG,
                    PROCESSING_UPLOADS_NOTIFICATION_ID,
                    processingUploads.build()
                )

                queuedContributions.asFlow().map { contribution ->
                    /**
                     * If the limited connection mode is on, lets iterate through the queued
                     * contributions
                     * and set the state as STATE_QUEUED_LIMITED_CONNECTION_MODE ,
                     * otherwise proceed with the upload
                     */
                    if(isLimitedConnectionModeEnabled()){
                        if (contribution.state == Contribution.STATE_QUEUED) {
                            contribution.state = Contribution.STATE_QUEUED_LIMITED_CONNECTION_MODE
                            contributionDao.save(contribution)
                        }
                    } else {
                        contribution.transferred = 0
                        contribution.state = Contribution.STATE_IN_PROGRESS
                        contributionDao.save(contribution)
                        uploadContribution(contribution = contribution)
                    }
                }.collect()

                //Dismiss the global notification
                notificationManager?.cancel(
                    PROCESSING_UPLOADS_NOTIFICATION_TAG,
                    PROCESSING_UPLOADS_NOTIFICATION_ID
                )

                //No need to keep looking if the limited connection mode is on,
                //If the user toggles it, the work manager will be started again
                if(isLimitedConnectionModeEnabled()){
                    break;
                }
            }
        }
        //TODO make this smart, think of handling retries in the future
        return Result.success()
    }

    /**
     * Returns true is the limited connection mode is enabled
     */
    private fun isLimitedConnectionModeEnabled(): Boolean {
        return sessionManager.getPreference(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED)
    }

    /**
     * Upload the contribution
     * @param contribution
     */
    @SuppressLint("StringFormatInvalid")
    private suspend fun uploadContribution(contribution: Contribution) {
        if (contribution.localUri == null || contribution.localUri.path == null) {
            Timber.e("""upload: ${contribution.media.filename} failed, file path is null""")
        }

        val media = contribution.media
        val displayTitle = contribution.media.displayTitle

        currentNotificationTag = contribution.localUri.toString()
        currentNotificationID =
            (contribution.localUri.toString() + contribution.media.filename).hashCode()

        curentNotification
        getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)!!
        curentNotification.setContentTitle(
            appContext.getString(
                R.string.upload_progress_notification_title_start,
                displayTitle
            )
        )

        notificationManager?.notify(
            currentNotificationTag,
            currentNotificationID,
            curentNotification.build()!!
        )

        val filename = media.filename

        val notificationProgressUpdater = NotificationUpdateProgressListener(
            appContext.getString(
                R.string.upload_progress_notification_title_finishing,
                displayTitle
            ),
            contribution
        )

        try {
            //Upload the file to stash
            val stashUploadResult = uploadClient.uploadFileToStash(
                appContext, filename, contribution, notificationProgressUpdater
            ).blockingSingle()

            when (stashUploadResult.state) {
                StashUploadState.SUCCESS -> {
                    //If the stash upload succeeds
                    Timber.d("Upload to stash success for fileName: $filename")
                    Timber.d("Ensure uniqueness of filename");
                    val uniqueFileName = findUniqueFileName(filename!!)


                    try {
                        //Upload the file from stash
                        val uploadResult = uploadClient.uploadFileFromStash(
                            contribution, uniqueFileName, stashUploadResult.fileKey
                        ).blockingSingle()

                        if (uploadResult.isSuccessful()) {
                            Timber.d(
                                "Stash Upload success..proceeding to make wikidata edit"
                            )

                            wikidataEditService.addDepictionsAndCaptions(uploadResult, contribution)
                                .blockingSubscribe();
                            if(contribution.wikidataPlace==null){
                                Timber.d(
                                    "WikiDataEdit not required, upload success"
                                )
                                saveCompletedContribution(contribution,uploadResult)
                                showSuccessNotification(contribution)
                            }else{
                                Timber.d(
                                    "WikiDataEdit not required, making wikidata edit"
                                )
                                makeWikiDataEdit(uploadResult, contribution)
                            }

                        } else {
                            Timber.e("Stash Upload failed")
                            showFailedNotification(contribution)
                            contribution.state = Contribution.STATE_FAILED
                            contribution.chunkInfo = null
                            contributionDao.save(contribution).blockingAwait()

                        }
                    }catch (exception : Exception){
                        Timber.e(exception)
                        Timber.e("Upload from stash failed for contribution : $filename")
                        showFailedNotification(contribution)
                        if (STASH_ERROR_CODES.contains(exception.message)) {
                            clearChunks(contribution)
                        }
                    }
                }
                StashUploadState.PAUSED -> {
                    showPausedNotification(contribution)
                    contribution.state = Contribution.STATE_PAUSED
                    contributionDao.save(contribution).blockingGet()
                }
                else -> {
                    Timber.e("""upload file to stash failed with status: ${stashUploadResult.state}""")
                    showFailedNotification(contribution)
                    contribution.state = Contribution.STATE_FAILED
                    contribution.chunkInfo = null
                    contributionDao.save(contribution).blockingAwait()
                }
            }
        }catch (exception: Exception){
            Timber.e(exception)
            Timber.e("Stash upload failed for contribution: $filename")
            showFailedNotification(contribution)
        }
    }

    private fun clearChunks(contribution: Contribution) {
        contribution.chunkInfo=null
        contributionDao.save(contribution).blockingAwait()
    }

    /**
     * Make the WikiData Edit, if applicable
     */
    private suspend fun makeWikiDataEdit(uploadResult: UploadResult, contribution: Contribution) {
        val wikiDataPlace = contribution.wikidataPlace
        if (wikiDataPlace != null && wikiDataPlace.imageValue == null) {
            if (!contribution.hasInvalidLocation()) {
                var revisionID: Long?=null
                try {
                    revisionID = wikidataEditService.createClaim(
                        wikiDataPlace, uploadResult.filename,
                        contribution.media.captions
                    )
                    if (null != revisionID) {
                        showSuccessNotification(contribution)
                    }
                }catch (exception: Exception){
                    Timber.e(exception)
                }

                withContext(Dispatchers.Main) {
                    wikidataEditService.handleImageClaimResult(
                        contribution.wikidataPlace,
                        revisionID
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    wikidataEditService.handleImageClaimResult(
                        contribution.wikidataPlace, null
                    )
                }
            }
        }
        saveCompletedContribution(contribution, uploadResult)
    }

    private fun saveCompletedContribution(contribution: Contribution, uploadResult: UploadResult) {
        val contributionFromUpload = mediaClient.getMedia("File:" + uploadResult.filename)
            .map { media: Media? -> contribution.completeWith(media!!) }
            .blockingGet()
        contributionFromUpload.dateModified=Date()
        contributionDao.deleteAndSaveContribution(contribution, contributionFromUpload)
    }

    private fun findUniqueFileName(fileName: String): String {
        var sequenceFileName: String?
        var sequenceNumber = 1
        while (true) {
            sequenceFileName = if (sequenceNumber == 1) {
                fileName
            } else {
                if (fileName.indexOf('.') == -1) {
                    "$fileName $sequenceNumber"
                } else {
                    val regex =
                        Pattern.compile("^(.*)(\\..+?)$")
                    val regexMatcher = regex.matcher(fileName)
                    regexMatcher.replaceAll("$1 $sequenceNumber$2")
                }
            }
            if (!mediaClient.checkPageExistsUsingTitle(
                    String.format(
                        "File:%s",
                        sequenceFileName
                    )
                )
                    .blockingGet()
            ) {
                break
            }
            sequenceNumber++
        }
        return sequenceFileName!!
    }

    /**
     * Notify that the current upload has succeeded
     * @param contribution
     */
    @SuppressLint("StringFormatInvalid")
    private fun showSuccessNotification(contribution: Contribution) {
        val displayTitle = contribution.media.displayTitle
        contribution.state=Contribution.STATE_COMPLETED
        curentNotification.setContentTitle(
            appContext.getString(
                R.string.upload_completed_notification_title,
                displayTitle
            )
        )
            .setContentText(appContext.getString(R.string.upload_completed_notification_text))
            .setProgress(0, 0, false)
            .setOngoing(false)
        notificationManager?.notify(
            currentNotificationTag, currentNotificationID,
            curentNotification.build()
        )
    }

    /**
     * Notify that the current upload has failed
     * @param contribution
     */
    @SuppressLint("StringFormatInvalid")
    private fun showFailedNotification(contribution: Contribution) {
        val displayTitle = contribution.media.displayTitle
        curentNotification.setContentTitle(
            appContext.getString(
                R.string.upload_failed_notification_title,
                displayTitle
            )
        )
            .setContentText(appContext.getString(R.string.upload_failed_notification_subtitle))
            .setProgress(0, 0, false)
            .setOngoing(false)
        notificationManager?.notify(
            currentNotificationTag, currentNotificationID,
            curentNotification.build()
        )
    }

    /**
     * Notify that the current upload is paused
     * @param contribution
     */
    private fun showPausedNotification(contribution: Contribution) {
        val displayTitle = contribution.media.displayTitle
        curentNotification.setContentTitle(
            appContext.getString(
                R.string.upload_paused_notification_title,
                displayTitle
            )
        )
            .setContentText(appContext.getString(R.string.upload_paused_notification_subtitle))
            .setProgress(0, 0, false)
            .setOngoing(false)
        notificationManager!!.notify(
            currentNotificationTag, currentNotificationID,
            curentNotification.build()
        )
    }
}