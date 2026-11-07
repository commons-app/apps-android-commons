package fr.free.nrw.commons.upload.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.UploadProgressActivity
import fr.free.nrw.commons.utils.getStringSafe
import javax.inject.Inject

/**
 * Handles creation, updates, and dispatching of upload-related notifications for [UploadWorker].
 */
class UploadNotificationHelper
    @Inject
    constructor(
        private val context: Context,
    ) {
        internal var notificationManager: NotificationManagerCompat =
            NotificationManagerCompat.from(context)

        // Attributes of the current-upload notification
        internal var currentNotificationID: Int = -1 // lateinit is not allowed with primitives
        internal lateinit var currentNotificationTag: String
        internal var currentNotification: NotificationCompat.Builder =
            getNotificationBuilder(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS)

        companion object {
            /** Id of the foreground service notification, which also shows the progress of the current upload */
            const val FOREGROUND_NOTIFICATION_ID = 1
            private const val PROGRESS_MAX = 100
            private const val PROGRESS_ZERO = 0
        }

        /**
         * Builds a notification builder pre-configured for upload progress with icons and flags.
         *
         * @param channelId the notification channel identifier
         * @return configured [NotificationCompat.Builder]
         */
        fun getNotificationBuilder(channelId: String): NotificationCompat.Builder =
            NotificationCompat
                .Builder(context, channelId)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.ic_launcher,
                    ),
                ).setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setProgress(PROGRESS_MAX, PROGRESS_ZERO, true)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        /**
         * Initializes and posts the initial notification when an upload begins.
         *
         * @param contribution the contribution being uploaded
         */
        @SuppressLint("MissingPermission")
        fun showUploadStarted(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotificationTag = contribution.localUri.toString()
            currentNotificationID =
                (contribution.localUri.toString() + contribution.media.filename).hashCode()

            // The previous upload may have left the notification on the result channel
            currentNotification.useUploadProgressChannel()
            currentNotification.setContentTitle(
                context.getStringSafe(
                    R.string.upload_progress_notification_title_start,
                    displayTitle,
                ),
            )
            // Show the progress in the foreground service notification instead of a second
            // notification, the result of the upload is shown in its own notification
            notificationManager.notify(
                FOREGROUND_NOTIFICATION_ID,
                currentNotification.build(),
            )
        }

        /**
         * Updates progress bar on the active upload notification.
         *
         * @param transferred number of bytes transferred
         * @param total total number of bytes to transfer
         * @param finishingTitle notification title to show when upload completes transfer
         * @param startingTag optional tag of starting notification to cancel
         * @param startingId optional id of starting notification to cancel
         */
        @SuppressLint("MissingPermission")
        fun updateProgress(
            transferred: Long,
            total: Long,
            finishingTitle: String?,
            startingTag: String? = null,
            startingId: Int? = null,
        ) {
            if (transferred == total) {
                // Completed!
                currentNotification
                    .setContentTitle(finishingTitle)
                    .setProgress(PROGRESS_ZERO, PROGRESS_MAX, true)
            } else {
                currentNotification
                    .setProgress(
                        PROGRESS_MAX,
                        (transferred.toDouble() / total.toDouble() * PROGRESS_MAX).toInt(),
                        false,
                    )
            }
            if (startingTag != null && startingId != null) {
                notificationManager.cancel(startingTag, startingId)
            }
            notificationManager.notify(
                FOREGROUND_NOTIFICATION_ID,
                currentNotification.build(),
            )
        }

        /**
         * Notify that the current upload has succeeded
         * @param contribution
         */
        @SuppressLint("StringFormatInvalid", "MissingPermission")
        fun showSuccessNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            contribution.state = Contribution.STATE_COMPLETED
            currentNotification.setContentIntent(getPendingIntent(MainActivity::class.java))
            currentNotification
                .setContentTitle(
                    context.getStringSafe(
                        R.string.upload_completed_notification_title,
                        displayTitle,
                    ),
                ).setContentText(context.getString(R.string.upload_completed_notification_text))
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
                .useUploadResultChannel()
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Notify that the current upload has failed
         * @param contribution
         */
        @SuppressLint("StringFormatInvalid", "MissingPermission")
        fun showFailedNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotification.setContentIntent(
                getPendingIntent(
                    UploadProgressActivity::class.java,
                    UploadProgressActivity.TAB_FAILED,
                ),
            )
            currentNotification
                .setContentTitle(
                    context.getStringSafe(
                        R.string.upload_failed_notification_title,
                        displayTitle,
                    ),
                ).setContentText(context.getString(R.string.upload_failed_notification_subtitle))
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
                .useUploadResultChannel()
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Shows a notification for a failed contribution upload.
         */
        @SuppressLint("StringFormatInvalid", "MissingPermission")
        fun showErrorNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotification.setContentIntent(
                getPendingIntent(
                    UploadProgressActivity::class.java,
                    UploadProgressActivity.TAB_FAILED,
                ),
            )
            currentNotification
                .setContentTitle(
                    context.getStringSafe(
                        R.string.upload_failed_notification_title,
                        displayTitle,
                    ),
                ).setContentText(contribution.errorInfo)
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
                .useUploadResultChannel()
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Notify that the current upload has failed due to invalid login
         * @param contribution
         */
        @SuppressLint("StringFormatInvalid", "MissingPermission")
        fun showInvalidLoginNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotification
                .setContentTitle(
                    context.getStringSafe(
                        R.string.upload_failed_notification_title,
                        displayTitle,
                    ),
                ).setContentText(context.getString(R.string.invalid_login_message))
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
                .useUploadResultChannel()
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Notify that the current upload is paused
         * @param contribution
         */
        @SuppressLint("MissingPermission")
        fun showPausedNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotification.setContentIntent(getPendingIntent(UploadProgressActivity::class.java))
            currentNotification
                .setContentTitle(
                    context.getStringSafe(
                        R.string.upload_paused_notification_title,
                        displayTitle,
                    ),
                ).setContentText(context.getString(R.string.upload_paused_notification_subtitle))
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Notify that the current upload is cancelled
         * @param contribution
         */
        @SuppressLint("MissingPermission")
        fun showCancelledNotification(contribution: Contribution) {
            val displayTitle = contribution.media.displayTitle
            currentNotification.setContentIntent(getPendingIntent(UploadProgressActivity::class.java))
            currentNotification
                .setContentTitle(displayTitle)
                .setContentText("Upload has been cancelled!")
                .setProgress(PROGRESS_ZERO, PROGRESS_ZERO, false)
                .setOngoing(false)
            notificationManager.notify(
                currentNotificationTag,
                currentNotificationID,
                currentNotification.build(),
            )
        }

        /**
         * Method used to get Pending intent for opening different screen after clicking on notification
         * @param toClass
         * @param targetTab
         */
        private fun getPendingIntent(
            toClass: Class<out BaseActivity>,
            targetTab: Int = 0,
        ): PendingIntent {
            val intent = Intent(context, toClass)
            intent.putExtra(UploadProgressActivity.EXTRA_TARGET_TAB, targetTab)
            return TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getPendingIntent(
                        targetTab,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                } else {
                    getPendingIntent(targetTab, PendingIntent.FLAG_UPDATE_CURRENT)
                }
            }
        }
    }
