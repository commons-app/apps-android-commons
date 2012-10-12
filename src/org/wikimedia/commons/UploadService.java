package org.wikimedia.commons;

import java.io.*;

import org.mediawiki.api.*;

import de.mastacode.http.ProgressListener;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.net.*;

public class UploadService extends IntentService {

    private static final String EXTRA_PREFIX = "org.wikimedia.commons.uploader";
    public static final String EXTRA_MEDIA_URI = EXTRA_PREFIX + ".media_uri";
    public static final String EXTRA_TARGET_FILENAME = EXTRA_PREFIX + ".filename";
    public static final String EXTRA_PAGE_CONTENT = EXTRA_PREFIX + ".content";
    public static final String EXTRA_EDIT_SUMMARY = EXTRA_PREFIX + ".summary";
   
    private NotificationManager notificationManager;
    public UploadService(String name) {
        super(name);
    }

    public UploadService() {
        super("UploadService");
    }
    public static final int NOTIFICATION_DOWNLOAD_IN_PROGRESS = 1;
    
    private class NotificationUpdateProgressListener implements ProgressListener {

        Notification curNotification;
        public NotificationUpdateProgressListener(Notification curNotification) {
            Log.d("Commons", "Fuckity");
            this.curNotification = curNotification;
        }
        @Override
        public void onProgress(long transferred, long total) {
            double percent = transferred/total * 100;
            Log.d("Commons", "Uploaded " + percent + "% (" + transferred + " of " + total + ")");
            curNotification.contentView.setProgressBar(R.id.uploadNotificationProgress, 100, (int)percent, false); 
            notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
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
    }

    @Override
    protected void onHandleIntent(Intent intent) {
       MWApi api = ((CommonsApplication)this.getApplicationContext()).getApi();
       InputStream file;
       ApiResult result;
       RemoteViews notificationView;
       
       Bundle extras = intent.getExtras();
       Uri mediaUri = (Uri)extras.getParcelable(EXTRA_MEDIA_URI);
       String filename = intent.getStringExtra(EXTRA_TARGET_FILENAME);
       String pageContents = intent.getStringExtra(EXTRA_PAGE_CONTENT);
       String editSummary = intent.getStringExtra(EXTRA_EDIT_SUMMARY);
       
       try {
           file =  this.getContentResolver().openInputStream(mediaUri);
       } catch (FileNotFoundException e) {
           throw new RuntimeException(e);
       }
            
       notificationView = new RemoteViews(getPackageName(), R.layout.layout_upload_progress);
       notificationView.setTextViewText(R.id.uploadNotificationTitle, "Uploading " + filename);
       
       Log.d("Commons", "Before execution!");
       Notification curNotification = new NotificationCompat.Builder(this).setAutoCancel(true)
               .setSmallIcon(R.drawable.ic_launcher)
               .setAutoCancel(true)
               .setContent(notificationView)
               .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0))
               .getNotification();
     
       notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
       
       Toast startingToast = Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG);
       startingToast.show(); 
       Log.d("Commons", "Just before");
       try {
           result = api.upload(filename, file, pageContents, editSummary, new NotificationUpdateProgressListener(curNotification));
       } catch (IOException e) {
           // Do error handling!
           Log.d("Commons", "Fuck");
           e.printStackTrace();
           throw new RuntimeException(e);
       }
       
       Log.d("Commons", "After");
       notificationView.setTextViewText(R.id.uploadNotificationTitle, filename + " Uploaded!");
       notificationManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
    }
}
