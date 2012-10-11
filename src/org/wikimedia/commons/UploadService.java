package org.wikimedia.commons;

import java.io.*;

import org.mediawiki.api.*;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import android.net.*;

public class UploadService extends Service {

    public static final int NOTIFICATION_DOWNLOAD_IN_PROGRESS = 1;
    
    class UploadImageTask extends AsyncTask<String, Integer, String> {
        MWApi api;
        Context context;
        NotificationManager notificationsManager;
       
        private Notification curNotification;
        
        @Override
        protected String doInBackground(String... params)  {
            Uri imageUri = Uri.parse(params[0]);
            String filename = params[1];
            String text = params[2];
            String comment = params[3];
            
            InputStream file;
            
            try {
                file =  context.getContentResolver().openInputStream(imageUri);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            try {
                ApiResult result = api.upload(filename, file, text, comment);
            
//                Document document = (Document)result.getDocument();
//                StringWriter wr = new StringWriter();
//                try {
//                    Transformer trans = TransformerFactory.newInstance().newTransformer();
//                    trans.transform(new DOMSource(result.getDocument()),   new StreamResult(wr));
//                    String res = wr.toString();
//                    return res;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    //FUCK YOU YOU SON OF A LEMON PARTY
//                }
                
                return result.getString("/api/upload/@result");
            } catch (IOException e) {
                e.printStackTrace();
                return "Failure";
            }
        }
        
        UploadImageTask(MWApi api, Context context, NotificationManager notificationManager) {
            this.api = api;
            this.context = context;
            this.notificationsManager = notificationManager;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("Commons", "Done!");
            super.onPostExecute(result);
            if(result.equals("Success")) {
                Toast successToast = Toast.makeText(context, R.string.uploading_success, Toast.LENGTH_LONG);
                successToast.show();
            } else {
                Toast failureToast = Toast.makeText(context, R.string.uploading_failed, Toast.LENGTH_LONG);
                failureToast.show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast startingToast = Toast.makeText(context, R.string.uploading_started, Toast.LENGTH_LONG);
            startingToast.show();
            Log.d("Commons", "Before execution!");
//            curNotification = new NotificationCompat.Builder(context).setAutoCancel(true)
//                    .setContentTitle("Starting Upload!")
//                    .setContentText("Uploading!")
//                    .setSmallIcon(R.drawable.ic_launcher)
//                    .setAutoCancel(true)
//                    .getNotification();
//            
//            notificationsManager.notify(NOTIFICATION_DOWNLOAD_IN_PROGRESS, curNotification);
        }
        
        
    }
    public class UploadBinder extends Binder {
        UploadService getService() {
            return UploadService.this;
        }
    }
   
    final UploadBinder uploadBinder = new UploadBinder();
    @Override
    public IBinder onBind(Intent intent) {
       return uploadBinder; 
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public void doUpload(MWApi api, Uri imageUri, String filename, String description, String editSummary) {
        UploadImageTask uploadImage = new UploadImageTask(api, this, (NotificationManager)getSystemService(NOTIFICATION_SERVICE));
        uploadImage.execute(imageUri.toString(), filename, description, editSummary);
    }
}
