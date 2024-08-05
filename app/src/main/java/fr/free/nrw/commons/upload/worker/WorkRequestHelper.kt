package fr.free.nrw.commons.upload.worker

import android.content.Context
import androidx.work.*
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Helper class for all the one time work requests
 */
class WorkRequestHelper {

    companion object {

        private var isUploadWorkerRunning = false
        private val lock = Object()

        fun makeOneTimeWorkRequest(context: Context, existingWorkPolicy: ExistingWorkPolicy) {

            synchronized(lock) {
                if (isUploadWorkerRunning) {
                    Timber.e("UploadWorker is already running. Cannot start another instance.")
                    return
                } else {
                    Timber.e("Setting isUploadWorkerRunning to true")
                    isUploadWorkerRunning = true
                }
            }

            /* Set backoff criteria for the work request
           The default backoff policy is EXPONENTIAL, but while testing we found that it
           too long for the uploads to finish. So, set the backoff policy as LINEAR with the
           minimum backoff delay value of 10 seconds.

           More details on when exactly it is retried:
           https://developer.android.com/guide/background/persistent/getting-started/define-work#retries_backoff
         */
            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val uploadRequest: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(UploadWorker::class.java)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .setConstraints(constraints)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UploadWorker::class.java.simpleName, existingWorkPolicy, uploadRequest
            )

        }

        /**
         * Sets the flag isUploadWorkerRunning to`false` allowing new worker to be started.
         */
        fun markUploadWorkerAsStopped() {
            synchronized(lock) {
                isUploadWorkerRunning = false
            }
        }
    }
}

