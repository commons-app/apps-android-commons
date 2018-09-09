package fr.free.nrw.commons.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import timber.log.Timber;

/**
 * This asynctask will check unread notifications after a date (date user check notifications last)
 */

public class UnreadNotificationsCheckAsync extends AsyncTask<Void, Void, List<Notification>> {

    WeakReference<ContributionsActivity> context;
    NotificationController notificationController;


    public UnreadNotificationsCheckAsync(ContributionsActivity context, NotificationController notificationController) {
        this.context = new WeakReference<>(context);
        this.notificationController = notificationController;
    }

    @Override
    protected List<Notification> doInBackground(Void... voids) {
        List<Notification>  unreadNotifications = null;

        Date currentDate = Calendar.getInstance().getTime();
        Date lastReadNotificationDateStored =
                new Date(context.get()
                        .getSharedPreferences("prefs",0)
                        .getLong("last_read_notification_date", 0));

        if (currentDate.after(lastReadNotificationDateStored)) {
            Timber.d("You may have unread notifications since"+lastReadNotificationDateStored);
            Log.d("deneme","You may have unread notifications since"+lastReadNotificationDateStored +"++curr date is:"+currentDate);

            //TODO: fetch latest notification of user and save latest notification date to shared preferences
            //TODO: pass latest notification update date here. So that you will get notifications after that date

            try {
                unreadNotifications = findUnreadNotifications(notificationController.getNotifications());
                Log.d("deneme","notifications is:"+unreadNotifications);
                for (Notification notification : unreadNotifications) {
                    Log.d("deneme", notification.notificationText);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // This case is not possible ever?
            Timber.d("You read all notifications of your at"+lastReadNotificationDateStored +"++curr date is:"+currentDate);
        }
        return unreadNotifications;
    }

    @Override
    protected void onPostExecute(List<Notification> unreadNotification) {
        super.onPostExecute(unreadNotification);

        // Check if activity is still running
        if (context.get().getWindow().getDecorView().isShown() && !context.get().isFinishing()) {
            // Check if fragment is not null and visible
            if (context.get().isContributionsFragmentVisible && context.get().contributionsActivityPagerAdapter.contributionsFragment != null) {
                (context.get().contributionsActivityPagerAdapter.contributionsFragment).updateNotificationsNotification(unreadNotification);
            }
        }
    }

    private List<Notification> findUnreadNotifications(List<Notification> allNotifications) {
        // TODO: only return notifications after last read date
        return allNotifications;
    }
}
