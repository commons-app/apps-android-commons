package org.wikimedia.commons;

import java.io.*;
import java.util.Date;

import android.graphics.BitmapFactory;
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

    public static final int ACTION_UPLOAD_FILE = 1;

    public static final String ACTION_START_SERVICE = EXTRA_PREFIX + ".upload";
    public static final String EXTRA_SOURCE = EXTRA_PREFIX + ".source";

    private NotificationManager notificationManager;
    private ContentProviderClient contributionsProviderClient;
    private CommonsApplication app;

    private NotificationCompat.Builder curProgressNotification;

    private int toUpload;

    // DO NOT HAVE NOTIFICATION ID OF 0 FOR ANYTHING
    // See http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
    // Seriously, Android?
    public static final int NOTIFICATION_UPLOAD_IN_PROGRESS = 1;
    public static final int NOTIFICATION_UPLOAD_COMPLETE = 2;
    public static final int NOTIFICATION_UPLOAD_FAILED = 3;

    private class NotificationUpdateProgressListener implements ProgressListener {

        String notificationTag;
        boolean notificationTitleChanged;
        Contribution contribution;

        String notificationProgressTitle;
        String notificationFinishingTitle;

        public NotificationUpdateProgressListener(String notificationTag, String notificationProgressTitle, String notificationFinishingTitle, Contribution contribution) {
            this.notificationTag = notificationTag;
            this.notificationProgressTitle = notificationProgressTitle;
            this.notificationFinishingTitle = notificationFinishingTitle;
            this.contribution = contribution;
        }

        @Override
        public void onProgress(long transferred, long total) {
            Log.d("Commons", String.format("Uploaded %d of %d", transferred, total));
            if(!notificationTitleChanged) {
                curProgressNotification.setContentTitle(notificationProgressTitle);
                notificationTitleChanged = true;
                contribution.setState(Contribution.STATE_IN_PROGRESS);
            }
            if(transferred == total) {
                // Completed!
                curProgressNotification.setContentTitle(notificationFinishingTitle);
                curProgressNotification.setProgress(0, 100, true);
            } else {
                curProgressNotification.setProgress(100, (int) (((double) transferred / (double) total) * 100), false);
            }
            startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());

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
                    curProgressNotification.setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload));
                    Log.d("Commons", String.format("%d uploads left", toUpload));
                    this.startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());
                }

                super.queue(what, contribution);
                break;
            default:
                throw new IllegalArgumentException("Unknown value for what");
        }

    }

    private boolean freshStart = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getAction() == ACTION_START_SERVICE && freshStart) {
            ContentValues failedValues = new ContentValues();
            failedValues.put(Contribution.Table.COLUMN_STATE, Contribution.STATE_FAILED);

            int updated = getContentResolver().update(ContributionsContentProvider.BASE_URI,
                    failedValues,
                    Contribution.Table.COLUMN_STATE + " = ? OR " + Contribution.Table.COLUMN_STATE + " = ?",
                    new String[]{ String.valueOf(Contribution.STATE_QUEUED), String.valueOf(Contribution.STATE_IN_PROGRESS) }
            );
            Log.d("Commons", "Set " + updated + " uploads to failed");
            Log.d("Commons", "Flags is" + flags + " id is" + startId);
            freshStart = false;
        }
        return START_REDELIVER_INTENT;
    }

    private void uploadContribution(Contribution contribution) {
        MWApi api = app.getApi();

        ApiResult result;
        InputStream file = null;

        String notificationTag = contribution.getLocalUri().toString();


        try {
            file = this.getContentResolver().openInputStream(contribution.getLocalUri());
        } catch(FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Log.d("Commons", "Before execution!");
        curProgressNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(String.format(getString(R.string.upload_progress_notification_title_start), contribution.getDisplayTitle()))
                .setContentText(getResources().getQuantityString(R.plurals.uploads_pending_notification_indicator, toUpload, toUpload))
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(String.format(getString(R.string.upload_progress_notification_title_in_progress), contribution.getDisplayTitle()));

        this.startForeground(NOTIFICATION_UPLOAD_IN_PROGRESS, curProgressNotification.build());

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
            NotificationUpdateProgressListener notificationUpdater = new NotificationUpdateProgressListener(notificationTag,
                    String.format(getString(R.string.upload_progress_notification_title_in_progress), contribution.getDisplayTitle()),
                    String.format(getString(R.string.upload_progress_notification_title_finishing), contribution.getDisplayTitle()),
                    contribution
            );
            result = api.upload(contribution.getFilename(), file, contribution.getDataLength(), contribution.getPageContents(), contribution.getEditSummary(), notificationUpdater);


            Log.d("Commons", "Response is" + Utils.getStringFromDOM(result.getDocument()));

            curProgressNotification = null;


            String resultStatus = result.getString("/api/upload/@result");
            if(!resultStatus.equals("Success")) {
                String errorCode = result.getString("/api/error/@code");
                showFailedNotification(contribution);
                EventLog.schema(CommonsApplication.EVENT_UPLOAD_ATTEMPT)
                        .param("username", app.getCurrentAccount().name)
                        .param("source", contribution.getSource())
                        .param("multiple", contribution.getMultiple())
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
                        .param("multiple", contribution.getMultiple())
                        .param("result", "success")
                        .log();
            }
        } catch(IOException e) {
            Log.d("Commons", "I have a network fuckup");
            showFailedNotification(contribution);
            return;
        } finally {
            toUpload--;
            if(toUpload == 0) {
                stopForeground(true);
            }
        }

    }

    private void showFailedNotification(Contribution contribution) {
        Notification failureNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ContributionsActivity.class), 0))
                .setTicker(String.format(getString(R.string.upload_failed_notification_title), contribution.getDisplayTitle()))
                .setContentTitle(String.format(getString(R.string.upload_failed_notification_title), contribution.getDisplayTitle()))
                .setContentText(getString(R.string.upload_failed_notification_subtitle))
                .build();
        notificationManager.notify(NOTIFICATION_UPLOAD_FAILED, failureNotification);

        contribution.setState(Contribution.STATE_FAILED);
        contribution.save();
    }
}
