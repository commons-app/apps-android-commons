package fr.free.nrw.commons.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import javax.inject.Inject
import javax.inject.Singleton
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import androidx.core.app.NotificationCompat.DEFAULT_ALL
import androidx.core.app.NotificationCompat.PRIORITY_HIGH

/**
 * Helper class that can be used to build a generic notification
 * Going forward all notifications should be built using this helper class
 */
@Singleton
class NotificationHelper @Inject constructor(
    context: Context
) {

    companion object {
        const val NOTIFICATION_DELETE = 1
        const val NOTIFICATION_EDIT_CATEGORY = 2
        const val NOTIFICATION_EDIT_COORDINATES = 3
        const val NOTIFICATION_EDIT_DESCRIPTION = 4
        const val NOTIFICATION_EDIT_DEPICTIONS = 5
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationBuilder: NotificationCompat.Builder = NotificationCompat
        .Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
        .setOnlyAlertOnce(true)

    /**
     * Public interface to build and show a notification in the notification bar
     * @param context passed context
     * @param notificationTitle title of the notification
     * @param notificationMessage message to be displayed in the notification
     * @param notificationId the notificationID
     * @param intent the intent to be fired when the notification is clicked
     */
    fun showNotification(
        context: Context,
        notificationTitle: String,
        notificationMessage: String,
        notificationId: Int,
        intent: Intent
    ) {
        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentTitle(notificationTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setSmallIcon(R.drawable.ic_launcher)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(context, 1, intent, flags)
        notificationBuilder.setContentIntent(pendingIntent)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
