package fr.free.nrw.commons.upload.worker

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest.Companion.MIN_BACKOFF_MILLIS
import com.mapbox.mapboxsdk.Mapbox.getApplicationContext
import java.util.concurrent.TimeUnit

/**
 * Helper class for all the one time work requests
 */
class WorkRequestHelper {

    companion object {
        fun makeOneTimeWorkRequest(existingWorkPolicy: ExistingWorkPolicy) {
            /* Set backoff criteria for the work request
           The default backoff policy is EXPONENTIAL, but while testing we found that it
           too long for the uploads to finish. So, set the backoff policy as LINEAR with the
           minimum backoff delay value of 10 seconds.

           More details on when exactly it is retried:
           https://developer.android.com/guide/background/persistent/getting-started/define-work#retries_backoff
         */
            val uploadRequest: OneTimeWorkRequest =
                OneTimeWorkRequest.Builder(UploadWorker::class.java)
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()
            WorkManager.getInstance(getApplicationContext()).enqueueUniqueWork(
                UploadWorker::class.java.simpleName, existingWorkPolicy, uploadRequest
            )
        }
    }

}