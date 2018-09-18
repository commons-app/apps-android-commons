package fr.free.nrw.commons.notification;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.contributions.ContributionsActivity;
import timber.log.Timber;

/**
 * This asynctask will check unread notifications after a date (date user check notifications last)
 */

public class UnreadNotificationsCheckAsync extends AsyncTask<Void, Void, Notification> {

    WeakReference<ContributionsActivity> context;
    NotificationController notificationController;


    public UnreadNotificationsCheckAsync(ContributionsActivity context, NotificationController notificationController) {
        this.context = new WeakReference<>(context);
        this.notificationController = notificationController;
    }

    @Override
    protected Notification doInBackground(Void... voids) {
        Notification  lastNotification = null;

        try {
            lastNotification = findLastNotification(notificationController.getNotifications());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lastNotification;
    }

    @Override
    protected void onPostExecute(Notification lastNotification) {
        super.onPostExecute(lastNotification);

        if (lastNotification == null) {
            return;
        }

        Date lastNotificationCheckDate = new Date(context.get()
                .getSharedPreferences("prefs",0)
                .getLong("last_read_notification_date", 0));
        Timber.d("You may have unread notifications since"+lastNotificationCheckDate);

        boolean isThereUnreadNotifications;

        Date lastReadNotificationDate = new java.util.Date(Long.parseLong(lastNotification.dateWithYear)*1000);

        if (lastNotificationCheckDate.before(lastReadNotificationDate)) {
            isThereUnreadNotifications = true;
        } else {
            isThereUnreadNotifications = false;
        }

        // Check if activity is still running
        if (context.get().getWindow().getDecorView().isShown() && !context.get().isFinishing()) {
            // Check if fragment is not null and visible
            if (context.get().isContributionsFragmentVisible && context.get().contributionsActivityPagerAdapter.contributionsFragment != null) {
                (context.get().contributionsActivityPagerAdapter.contributionsFragment).updateNotificationsNotification(isThereUnreadNotifications);
            }
        }
    }

    private Notification findLastNotification(List<Notification> allNotifications) {
        if (allNotifications.size() > 0) {
            return allNotifications.get(allNotifications.size()-1);
        } else {
            return null;
        }
    }
}
