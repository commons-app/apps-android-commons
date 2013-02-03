package org.wikimedia.commons;

import java.io.*;
import java.text.*;
import java.util.Date;

import android.support.v4.content.LocalBroadcastManager;
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

public class UploadService extends IntentService {

    private static final String EXTRA_PREFIX = "org.wikimedia.commons.upload";
    public static final String INTENT_CONTRIBUTION_STATE_CHANGED = EXTRA_PREFIX + ".progress";
    public static final String EXTRA_CONTRIBUTION_ID = EXTRA_PREFIX + ".filename";
    public static final String EXTRA_TRANSFERRED_BYTES = EXTRA_PREFIX + ".progress.transferred";


    public static final String EXTRA_MEDIA_URI = EXTRA_PREFIX + ".uri";
    public static final String EXTRA_TARGET_FILENAME = EXTRA_PREFIX + ".filename";
    public static final String EXTRA_DESCRIPTION = EXTRA_PREFIX + ".description";
    public static final String EXTRA_EDIT_SUMMARY = EXTRA_PREFIX + ".summary";
    public static final String EXTRA_MIMETYPE = EXTRA_PREFIX + ".mimetype";

    private NotificationManager notificationManager;
    private LocalBroadcastManager localBroadcastManager;
    private ContentProviderClient contributionsProviderClient;
    private CommonsApplication app;

    private Notification curProgressNotification;

    private int toUpload;

    public UploadService(String name) {
        super(name);
    }

    public UploadService() {
        super("UploadService");
    }

    // DO NOT HAVE NOTIFICATION ID OF 0 FOR ANYTHING
    // See http://stackoverflow.com/questions/8725909/startforeground-does-not-show-my-notification
    // Seriously, Android?
    public static final int NOTIFICATION_DOWNLOAD_IN_PROGRESS = 1;
    public static final int NOTIFICATION_DOWNLOAD_COMPLETE = 2;
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
                contribution.save();
            }
            if(transferred == total) {
                // Completed!
                curView.setTextViewText(R.id.uploadNotificationTitle, notificationFinishingTitle);
                notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
            } else {
                curNotification.contentView.setProgressBar(R.id.uploadNotificationProgress, 100, (int) (((double) transferred / (double) total) * 100), false);
                notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);

                Intent mediaUploadProgressIntent = new Intent(INTENT_CONTRIBUTION_STATE_CHANGED);
                // mediaUploadProgressIntent.putExtra(EXTRA_MEDIA contribution);
                mediaUploadProgressIntent.putExtra(EXTRA_TRANSFERRED_BYTES, transferred);
                localBroadcastManager.sendBroadcast(mediaUploadProgressIntent);
            }
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
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        app = (CommonsApplication) this.getApplicationContext();
        contributionsProviderClient = this.getContentResolver().acquireContentProviderClient(ContributionsContentProvider.AUTHORITY);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private Contribution mediaFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        Uri mediaUri = (Uri) extras.getParcelable(EXTRA_MEDIA_URI);
        String filename = intent.getStringExtra(EXTRA_TARGET_FILENAME);
        String description = intent.getStringExtra(EXTRA_DESCRIPTION);
        String editSummary = intent.getStringExtra(EXTRA_EDIT_SUMMARY);
        String mimeType = intent.getStringExtra(EXTRA_MIMETYPE);
        Date dateCreated = null;

        Long length = null;
        try {
            length = this.getContentResolver().openAssetFileDescriptor(mediaUri, "r").getLength();
        } catch(FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        Log.d("Commons", "MimeType is " + mimeType);
        if(mimeType.startsWith("image/")) {
            Cursor cursor = this.getContentResolver().query(mediaUri,
                    new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
            if(cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                dateCreated = new Date(cursor.getLong(0));
            } // FIXME: Alternate way of setting dateCreated if this data is not found
        } /* else if (mimeType.startsWith("audio/")) {
             Removed Audio implementationf or now
           }  */
        Contribution contribution = new Contribution(mediaUri, null, filename, description, null, length, dateCreated, null, app.getCurrentAccount().name, editSummary);
        return contribution;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        toUpload++;
        if(curProgressNotification != null && toUpload != 1) {
            curProgressNotification.contentView.setTextViewText(R.id.uploadNotificationsCount, String.format(getString(R.string.uploads_pending_notification_indicator), toUpload));
            Log.d("Commons", String.format("%d uploads left", toUpload));
            notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curProgressNotification);
        }

        Contribution contribution = mediaFromIntent(intent);
        contribution.setState(Contribution.STATE_QUEUED);
        contribution.setContentProviderClient(contributionsProviderClient);

        contribution.save();

        Intent mediaUploadQueuedIntent = new Intent();
        mediaUploadQueuedIntent.putExtra("dummy-data", contribution); // FIXME: Move to separate handler, do not inherit from IntentService
        localBroadcastManager.sendBroadcast(mediaUploadQueuedIntent);
        return super.onStartCommand(mediaUploadQueuedIntent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MWApi api = app.getApi();

        ApiResult result;
        RemoteViews notificationView;
        Contribution contribution;
        InputStream file = null;
        contribution = (Contribution) intent.getSerializableExtra("dummy-data");

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

        this.startForeground(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curProgressNotification);

        Log.d("Commons", "Just before");

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

        Log.d("Commons", "Response is" + CommonsApplication.getStringFromDOM(result.getDocument()));
        stopForeground(true);
        curProgressNotification = null;

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Assuming MW always gives me UTC

        String resultStatus = result.getString("/api/upload/@result");
        if(!resultStatus.equals("success")) {
            showFailedNotification(contribution);
        } else {
            Date dateUploaded = null;
            try {
                dateUploaded = isoFormat.parse(result.getString("/api/upload/imageinfo/@timestamp"));
            } catch(java.text.ParseException e) {
                throw new RuntimeException(e); // Hopefully mediawiki doesn't give me bogus stuff?
            }
            contribution.setState(Contribution.STATE_COMPLETED);
            contribution.setDateUploaded(dateUploaded);
            contribution.save();
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
