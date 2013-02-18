package org.wikimedia.commons;

import java.io.*;
import java.util.Date;

import org.mediawiki.api.*;
import org.wikimedia.commons.contributions.Contribution;
import org.wikimedia.commons.contributions.ContributionsActivity;
import org.wikimedia.commons.contributions.ContributionsContentProvider;

import in.yuvi.http.fluent.ProgressListener;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.net.*;

public class UploadService extends HandlerService<Contribution> {

    private static final String EXTRA_PREFIX = "org.wikimedia.commons.upload";

    public static final String EXTRA_MEDIA_URI = EXTRA_PREFIX + ".uri";
    public static final String EXTRA_TARGET_FILENAME = EXTRA_PREFIX + ".filename";
    public static final String EXTRA_DESCRIPTION = EXTRA_PREFIX + ".description";
    public static final String EXTRA_EDIT_SUMMARY = EXTRA_PREFIX + ".summary";
    public static final String EXTRA_MIMETYPE = EXTRA_PREFIX + ".mimetype";

    public static final int ACTION_UPLOAD_FILE = 1;

    public static final String ACTION_START_SERVICE = EXTRA_PREFIX + ".upload";
    public static final String EXTRA_SOURCE = EXTRA_PREFIX + ".source";

    private NotificationManager notificationManager;
    private ContentProviderClient contributionsProviderClient;
    private CommonsApplication app;

    private Notification curProgressNotification;

    private int toUpload;

    // DO NOT HAVE NOTIFICATION ID OF 0 FOR ANYTHING
    // See http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
    // Seriously, Android?
    public static final int NOTIFICATION_UPLOAD_IN_PROGRESS = 1;
    public static final int NOTIFICATION_UPLOAD_COMPLETE = 2;
    public static final int NOTIFICATION_UPLOAD_FAILED = 3;

    private class NotificationUpdateProgressListener implements ProgressListener {

        Notification curNotification;
        String notificationTag;
        boolean notificationTitleChanged;
        Contribution contribution;

        String notificationProgressTitle;
        String notificationFinishingTitle;

        public NotificationUpdateProgressListener(Notification curNotification, String notificationTag, String notificationProgressTitle, String notificationFinishingTitle, Contribution contribution) {
            this.curNotification = curNotification;
            this.notificationTag = notificationTag;
            this.notificationProgressTitle = notificationProgressTitle;
            this.notificationFinishingTitle = notificationFinishingTitle;
            this.contribution = contribution;
        }

        @Override
        public void onProgress(long transferred, long total) {
            Log.d("Commons", String.format("Uploaded %d of %d", transferred, total));
            RemoteViews curView = curNotification.contentView;
            if(!notificationTitleChanged) {
                curView.setTextViewText(R.id.uploadNotificationTitle, notificationProgressTitle);
                if(toUpload != 1) {
                    curView.setTextViewText(R.id.uploadNotificationsCount, String.format(getString(R.string.uploads_pending_notification_indicator), toUpload));
                    Log.d("Commons", String.format("%d uploads left", toUpload));
                }
                notificationTitleChanged = true;
                contribution.setState(Contribution.STATE_IN_PROGRESS);
            }
            if(transferred == total) {
                // Completed!
                curView.setTextViewText(R.id.uploadNotificationTitle, notificationFinishingTitle);
                notificationManager.notify(NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification);
            } else {
                curNotification.contentView.setProgressBar(R.id.uploadNotificationProgress, 100, (int) (((double) transferred / (double) total) * 100), false);
                notificationManager.notify(NOTIFICATION_UPLOAD_IN_PROGRESS, curNotification);
            }
            contribution.setTransferred(transferred);
            contribution.save();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contributionsProviderClient.release();
        Log.d("Commons", "ZOMG I AM BEING KILLED HALP!");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        app = (CommonsApplication) this.getApplicationContext();
        contributionsProviderClient = this.getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY);
    }

    @Override
    protected void handle(int what, Contribution contribution) {
        switch(what) {
            case ACTION_UPLOAD_FILE:
                uploadContribution(contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }
    }

    @Override
    public void queue(int what, Contribution contribution) {
        switch (what) {
            case ACTION_UPLOAD_FILE:

                contribution.setState(Contribution.STATE_QUEUED);
                contribution.setTransferred(0);
                contribution.setContentProviderClient(contributionsProviderClient);

                contribution.save();
                toUpload++;
                if (curProgressNotification != null && toUpload != 1) {
                    curProgressNotification.contentView.setTextViewText(R.id.uploadNotificationsCount, String.format(getString(R.string.uploads_pending_notification_indicator), toUpload));
                    Log.d("Commons", String.format("%d uploads left", toUpload));
                    notificationManager.notify(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification);
                }

                super.queue(what, contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() == ACTION_START_SERVICE) {
            ContentValues failedValues = new ContentValues();
            failedValues.put(Contribution.Table.COLUMN_STATE, Contribution.STATE_FAILED);

            int updated = getContentResolver().update(ContributionsContentProvider.BASE_URI,
                    failedValues,
                    Contribution.Table.COLUMN_STATE + " = ? OR " + Contribution.Table.COLUMN_STATE + " = ?",
                    new String[]{ String.valueOf(Contribution.STATE_QUEUED), String.valueOf(Contribution.STATE_IN_PROGRESS) }
            );
            Log.d("Commons", "Set " + updated + " uploads to failed");
        }
        return START_REDELIVER_INTENT;
    }

    private void uploadContribution(Contribution contribution) {
        MWApi api = app.getApi();

        ApiResult result;
        RemoteViews notificationView;
        InputStream file = null;

        String notificationTag = contribution.getLocalUri().toString();


        try {
            file = this.getContentResolver().openInputStream(contribution.getLocalUri());
        } catch(FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        notificationView = new RemoteViews(getPackageName(), R.layout.layout_upload_progress);
        notificationView.setTextViewText(R.id.uploadNotificationTitle, String.format(getString(R.string.upload_progress_notification_title_start), contribution.getFilename()));
        notificationView.setProgressBar(R.id.uploadNotificationProgress, 100, 0, false);

        Log.d("Commons", "Before execution!");
        curProgressNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContent(notificationView)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(String.format(getString(R.string.upload_progress_notification_title_in_progress), contribution.getFilename()))
                .getNotification();

        this.startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification);

        try {
            if(!api.validateLogin()) {
                // Need to revalidate!
                if(app.revalidateAuthToken()) {
                    Log.d("Commons", "Successfully revalidated token!");
                } else {
                    Log.d("Commons", "Unable to revalidate :(");
                    // TODO: Put up a new notification, ask them to re-login
                    stopForeground(true);
                    Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
                    failureToast.show();
                    return;
                }
            }
            NotificationUpdateProgressListener notificationUpdater = new NotificationUpdateProgressListener(curProgressNotification, notificationTag,
                    String.format(getString(R.string.upload_progress_notification_title_in_progress), contribution.getFilename()),
                    String.format(getString(R.string.upload_progress_notification_title_finishing), contribution.getFilename()),
                    contribution
            );
            result = api.upload(contribution.getFilename(), file, contribution.getDataLength(), contribution.getPageContents(), contribution.getEditSummary(), notificationUpdater);
        } catch(IOException e) {
            Log.d("Commons", "I have a network fuckup");
            showFailedNotification(contribution);
            return;
        } finally {
            toUpload--;
        }

        Log.d("Commons", "Response is" + Utils.getStringFromDOM(result.getDocument()));
        stopForeground(true);
        curProgressNotification = null;


        String resultStatus = result.getString("/api/upload/@result");
        if(!resultStatus.equals("Success")) {
            String errorCode = result.getString("/api/error/@code");
            showFailedNotification(contribution);
            EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("source", contribution.getSource())
                    .param("result", errorCode)
                    .param("filename", contribution.getFilename())
                    .log();
        } else {
            Date dateUploaded = null;
            dateUploaded = Utils.parseMWDate(result.getString("/api/upload/imageinfo/@timestamp"));
            String canonicalFilename = "File:" + result.getString("/api/upload/@filename").replace("_", " "); // Title vs Filename
            String imageUrl = result.getString("/api/upload/imageinfo/@url");
            contribution.setFilename(canonicalFilename);
            contribution.setImageUrl(imageUrl);
            contribution.setState(Contribution.STATE_COMPLETED);
            contribution.setDateUploaded(dateUploaded);
            contribution.save();

            EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                    .param("username", app.getCurrentAccount().name)
                    .param("source", contribution.getSource()) //FIXME
                    .param("filename", contribution.getFilename())
                    .param("result", "success")
                    .log();
        }
    }

    private void showFailedNotification(Contribution contribution) {
        stopForeground(true);
        Notification failureNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(String.format(getString(R.string.upload_failed_notification_title), contribution.getFilename()))
                .setContentTitle(String.format(getString(R.string.upload_failed_notification_title), contribution.getFilename()))
                .setContentText(getString(R.string.upload_failed_notification_subtitle))
                .getNotification();
        notificationManager.notify(NOTIFICATION_UPLOAD_FAILED, failureNotification);

        contribution.setState(Contribution.STATE_FAILED);
        contribution.save();
    }
}
