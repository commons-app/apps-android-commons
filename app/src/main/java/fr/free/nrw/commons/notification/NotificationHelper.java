package fr.free.nrw.commons.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import androidx.core.app.NotificationCompat;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

/**
 * Helper class that can be used to build a generic notification
 * Going forward all notifications should be built using this helper class
 */
@Singleton
public class NotificationHelper {

    public static final int NOTIFICATION_DELETE = 1;
    public static final int NOTIFICATION_EDIT_CATEGORY = 2;
    public static final int NOTIFICATION_EDIT_COORDINATES = 3;
    public static final int NOTIFICATION_EDIT_DESCRIPTION = 4;
    public static final int NOTIFICATION_EDIT_DEPICTIONS = 5;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Inject
    public NotificationHelper(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat
                .Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
                .setOnlyAlertOnce(true);
    }

    /**
     * Public interface to build and show a notification in the notification bar
     * @param context passed context
     * @param notificationTitle title of the notification
     * @param notificationMessage message to be displayed in the notification
     * @param notificationId the notificationID
     * @param intent the intent to be fired when the notification is clicked
     */
    public void showNotification(Context context,
        String notificationTitle,
        String notificationMessage,
        int notificationId,
        Intent intent) {

        notificationBuilder.setDefaults(DEFAULT_ALL)
            .setContentTitle(notificationTitle)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(notificationMessage))
            .setSmallIcon(R.drawable.ic_launcher)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(PRIORITY_HIGH);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        // Check if the API level is 31 or higher to modify the flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For API level 31 or above, PendingIntent requires either FLAG_IMMUTABLE or FLAG_MUTABLE to be set
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, flags);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
