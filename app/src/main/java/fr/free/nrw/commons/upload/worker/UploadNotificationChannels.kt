package fr.free.nrw.commons.upload.worker

import androidx.core.app.NotificationCompat
import fr.free.nrw.commons.CommonsApplication

/**
 * Puts an upload notification on the silent uploads channel, so that progress updates do not make
 * a sound every time they are posted.
 */
internal fun NotificationCompat.Builder.useUploadProgressChannel(): NotificationCompat.Builder =
    setChannelId(CommonsApplication.NOTIFICATION_CHANNEL_ID_UPLOADS)
        .setOnlyAlertOnce(true)

/**
 * Puts an upload notification on the default channel and lets it alert once, for results that the
 * user should notice, such as a finished or failed upload.
 */
internal fun NotificationCompat.Builder.useUploadResultChannel(): NotificationCompat.Builder =
    setChannelId(CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
        .setOnlyAlertOnce(false)
