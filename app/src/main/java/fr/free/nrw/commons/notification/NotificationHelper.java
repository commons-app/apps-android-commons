package fr.free.nrw.commons.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.app.NotificationCompat;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

@Singleton
public class NotificationHelper {

    public static final int NOTIFICATION_DELETE = 1;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Inject
    public NotificationHelper(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat
                .Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
                .setOnlyAlertOnce(true);
    }

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

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
