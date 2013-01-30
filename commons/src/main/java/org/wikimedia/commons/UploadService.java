package org.wikimedia.commons;

import java.io.*;
import java.util.Date;

import android.support.v4.content.LocalBroadcastManager;
import org.mediawiki.api.*;
import org.wikimedia.commons.media.Media;

import in.yuvi.http.fluent.ProgressListener;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.net.*;

public class UploadService extends IntentService {

    private static final String EXTRA_PREFIX = "org.wikimedia.commons.upload";
    public static final String INTENT_UPLOAD_COMPLETE = EXTRA_PREFIX + ".completed";
    public static final String INTENT_UPLOAD_STARTED = EXTRA_PREFIX + ".started";
    public static final String INTENT_UPLOAD_QUEUED = EXTRA_PREFIX + ".queued";
    public static final String INTENT_UPLOAD_PROGRESS = EXTRA_PREFIX + ".progress";
    public static final String EXTRA_MEDIA = ".media";
    public static final String EXTRA_TRANSFERRED_BYTES = ".progress.transferred";

    private NotificationManager notificationManager;
    private LocalBroadcastManager localBroadcastManager;
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
        Media media;
       
        String notificationProgressTitle;
        String notificationFinishingTitle;

        public NotificationUpdateProgressListener(Notification curNotification, String notificationTag, String notificationProgressTitle, String notificationFinishingTitle, Media media) {
            this.curNotification = curNotification;
            this.notificationTag = notificationTag;
            this.notificationProgressTitle = notificationProgressTitle;
            this.notificationFinishingTitle = notificationFinishingTitle;
            this.media = media;
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
                Intent mediaUploadStartedEvent = new Intent(INTENT_UPLOAD_STARTED);
                mediaUploadStartedEvent.putExtra(EXTRA_MEDIA, media);
                localBroadcastManager.sendBroadcast(mediaUploadStartedEvent);
            }
            if(transferred == total) {
                // Completed!
                curView.setTextViewText(R.id.uploadNotificationTitle, notificationFinishingTitle);
                notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
            } else {
                curNotification.contentView.setProgressBar(R.id.uploadNotificationProgress, 100, (int)(((double)transferred / (double)total) * 100), false);
                notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
                Intent mediaUploadProgressIntent = new Intent(INTENT_UPLOAD_PROGRESS);
                mediaUploadProgressIntent.putExtra(EXTRA_MEDIA, media);
                mediaUploadProgressIntent.putExtra(EXTRA_TRANSFERRED_BYTES, transferred);
                localBroadcastManager.sendBroadcast(mediaUploadProgressIntent);
            }
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Commons", "ZOMG I AM BEING KILLED HALP!");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        app = (CommonsApplication)this.getApplicationContext();
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private Media mediaFromIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        Uri mediaUri = (Uri)extras.getParcelable(Media.EXTRA_MEDIA_URI);
        String filename = intent.getStringExtra(Media.EXTRA_TARGET_FILENAME);
        String description = intent.getStringExtra(Media.EXTRA_DESCRIPTION);
        String editSummary = intent.getStringExtra(Media.EXTRA_EDIT_SUMMARY);
        String mimeType = intent.getStringExtra(Media.EXTRA_MIMETYPE);
        Date dateCreated = null;

        Long length = null;
        try {
            length = this.getContentResolver().openAssetFileDescriptor(mediaUri, "r").getLength();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        Log.d("Commons", "MimeType is " + mimeType);
        if (mimeType.startsWith("image/")) {
            Cursor cursor = this.getContentResolver().query(mediaUri,
                    new String[]{MediaStore.Images.ImageColumns.DATE_TAKEN}, null, null, null);
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                dateCreated = new Date(cursor.getLong(0));
            }
        } /* else if (mimeType.startsWith("audio/")) {
             Removed Audio implementationf or now
           }  */
        Media media = new Media(mediaUri, filename, description, editSummary, app.getCurrentAccount().name, dateCreated, length);
        return media;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        toUpload++;
        if(curProgressNotification != null && toUpload != 1) {
            curProgressNotification.contentView.setTextViewText(R.id.uploadNotificationsCount, String.format(getString(R.string.uploads_pending_notification_indicator), toUpload));
            Log.d("Commons", String.format("%d uploads left", toUpload));
            notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curProgressNotification);
        }

        Media media = mediaFromIntent(intent);

        Intent mediaUploadQueuedIntent = new Intent(INTENT_UPLOAD_QUEUED);
        mediaUploadQueuedIntent.putExtra(EXTRA_MEDIA, media);
        localBroadcastManager.sendBroadcast(mediaUploadQueuedIntent);
        return super.onStartCommand(mediaUploadQueuedIntent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
       MWApi api = app.getApi();

       ApiResult result;
       RemoteViews notificationView;
       Media media;
       InputStream file = null;
       media = (Media)intent.getParcelableExtra(EXTRA_MEDIA);

       String notificationTag = media.getMediaUri().toString();


        try {
            file =  this.getContentResolver().openInputStream(media.getMediaUri());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

       notificationView = new RemoteViews(getPackageName(), R.layout.layout_upload_progress);
       notificationView.setTextViewText(R.id.uploadNotificationTitle, String.format(getString(R.string.upload_progress_notification_title_start), media.getFileName()));
       notificationView.setProgressBar(R.id.uploadNotificationProgress, 100, 0, false);
       
       Log.d("Commons", "Before execution!");
       curProgressNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
               .setSmallIcon(R.drawable.ic_launcher)
               .setAutoCancel(true)
               .setContent(notificationView)
               .setOngoing(true)
               .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0))
               .setTicker(String.format(getString(R.string.upload_progress_notification_title_in_progress), media.getFileName()))
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
                   String.format(getString(R.string.upload_progress_notification_title_in_progress), media.getFileName()),
                   String.format(getString(R.string.upload_progress_notification_title_finishing), media.getFileName()),
                   media
           );
           result = api.upload(media.getFileName(), file, media.getLength(), media.getPageContents(), media.getEditSummary(), notificationUpdater);
       } catch (IOException e) {
           Log.d("Commons", "I have a network fuckup");
           stopForeground(true);
           Notification failureNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
                   .setSmallIcon(R.drawable.ic_launcher)
                   .setAutoCancel(true)
                   .setContentIntent(PendingIntent.getService(getApplicationContext(), 0, intent, 0))
                   .setTicker(String.format(getString(R.string.upload_failed_notification_title), media.getFileName()))
                   .setContentTitle(String.format(getString(R.string.upload_failed_notification_title), media.getFileName()))
                   .setContentText(getString(R.string.upload_failed_notification_subtitle))
                   .getNotification();
           notificationManager.notify(NOTIFICATION_UPLOAD_FAILED, failureNotification);
           return;
       } finally {
           toUpload--;
       }
      
       Log.d("Commons", "Response is"  + CommonsApplication.getStringFromDOM(result.getDocument()));
       stopForeground(true);
       curProgressNotification = null;
       
       String descUrl = result.getString("/api/upload/imageinfo/@descriptionurl");
       
       Intent openUploadedPageIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(descUrl));
       Notification doneNotification = new NotificationCompat.Builder(this)
               .setAutoCancel(true)
               .setSmallIcon(R.drawable.ic_launcher)
               .setContentTitle(String.format(getString(R.string.upload_completed_notification_title), media.getFileName()))
               .setContentText(getString(R.string.upload_completed_notification_text))
               .setTicker(String.format(getString(R.string.upload_completed_notification_title), media.getFileName()))
               .setContentIntent(PendingIntent.getActivity(this, 0, openUploadedPageIntent, 0))
               .getNotification();
       
       notificationManager.notify(notificationTag, NOTIFICATION_DOWNLOAD_COMPLETE, doneNotification);

       Intent mediaUploadedIntent = new Intent(INTENT_UPLOAD_COMPLETE);
       mediaUploadedIntent.putExtra(EXTRA_MEDIA, media);
       localBroadcastManager.sendBroadcast(mediaUploadedIntent);
    }
}
