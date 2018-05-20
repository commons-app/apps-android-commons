package fr.free.nrw.commons.review;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

import java.io.IOException;

import javax.inject.Inject;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static android.support.v4.app.NotificationCompat.DEFAULT_ALL;
import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;

// example code:
//
//                media = new Media("File:Iru.png");
//                        Observable.fromCallable(() -> mwApi.firstRevisionOfFile(media.getFilename()))
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(revision -> {
//                        SendThankTask task = new SendThankTask(getActivity(), media, revision);
//                        task.execute();
//                        });

public class SendThankTask extends AsyncTask<Void, Integer, Boolean> {

    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;

    public static final int NOTIFICATION_SEND_THANK = 0x102;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Context context;
    private Media media;
    private String revision;

    public SendThankTask(Context context, Media media){
        this.context = context;
        this.media = media;

    }

    @Override
    protected void onPreExecute(){
        ApplicationlessInjection
                .getInstance(context.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER,0,0);
        toast = Toast.makeText(context, context.getString(R.string.send_thank_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    protected Boolean doInBackground(Void ...voids) {
        publishProgress(0);

        String editToken;
        String authCookie;

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        try {
            this.revision =  mwApi.firstRevisionOfFile(media.getFilename());
            editToken = mwApi.getEditToken();
            if (editToken.equals("+\\")) {
                return false;
            }
            publishProgress(1);

            mwApi.thank(editToken, revision);

            publishProgress(2);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate (Integer... values){
        super.onProgressUpdate(values);

        int[] messages = new int[]{R.string.getting_edit_token, R.string.send_thank_send};
        String message = "";
        if (0 < values[0] && values[0] < messages.length) {
            message = context.getString(messages[values[0]]);
        }

        notificationBuilder.setContentTitle(context.getString(R.string.send_thank_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(messages.length, values[0], false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_SEND_THANK, notificationBuilder.build());
    }

    @Override
    protected void onPostExecute(Boolean result) {
        String message = "";
        String title = "";

        if (result){
            title = context.getString(R.string.send_thank_success_title);
            message = context.getString(R.string.send_thank_success_message, media.getDisplayTitle());
        }
        else {
            title = context.getString(R.string.send_thank_failure_title);
            message = context.getString(R.string.send_thank_failure_message, media.getDisplayTitle());
        }

        notificationBuilder.setDefaults(DEFAULT_ALL)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0,0,false)
                .setOngoing(false)
                .setPriority(PRIORITY_HIGH);
        notificationManager.notify(NOTIFICATION_SEND_THANK, notificationBuilder.build());
    }
}