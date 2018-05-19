package fr.free.nrw.commons.delete;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.view.Gravity;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static android.support.v4.app.NotificationCompat.DEFAULT_ALL;
import static android.support.v4.app.NotificationCompat.PRIORITY_HIGH;

public class DeleteTask extends AsyncTask<Void, Integer, Boolean> {

    @Inject MediaWikiApi mwApi;
    @Inject SessionManager sessionManager;

    public static final int NOTIFICATION_DELETE = 1;

    private NotificationManager notificationManager;
    private Builder notificationBuilder;
    private Context context;
    private Media media;
    private String reason;

    public DeleteTask (Context context, Media media, String reason){
        this.context = context;
        this.media = media;
        this.reason = reason;
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
        toast = Toast.makeText(context,"Trying to nominate "+media.getDisplayTitle()+ " for deletion", Toast.LENGTH_SHORT);
        toast.show();
    }

    @Override
    protected Boolean doInBackground(Void ...voids) {
        publishProgress(0);

        String editToken;
        String authCookie;
        String summary = context.getString(R.string.nominating_file_for_deletion, media.getFilename());

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        Calendar calendar = Calendar.getInstance();
        String fileDeleteString = "{{delete|reason=" + reason +
                "|subpage=" +media.getFilename() +
                "|day=" + calendar.get(Calendar.DAY_OF_MONTH) +
                "|month=" + calendar.getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.getDefault()) +
                "|year=" + calendar.get(Calendar.YEAR) +
                "}}";

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";

        String logPageString = "\n{{Commons:Deletion requests/" + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String date = sdf.format(calendar.getTime());

        String userPageString = "\n{{subst:idw|" + media.getFilename() +
                "}} ~~~~";

        try {
            editToken = mwApi.getEditToken();
            if (editToken.equals("+\\")) {
                return false;
            }
            publishProgress(1);

            mwApi.prependEdit(editToken,fileDeleteString+"\n",
                    media.getFilename(), summary);
            publishProgress(2);

            mwApi.edit(editToken,subpageString+"\n",
                    "Commons:Deletion_requests/"+media.getFilename(), summary);
            publishProgress(3);

            mwApi.appendEdit(editToken,logPageString+"\n",
                    "Commons:Deletion_requests/"+date, summary);
            publishProgress(4);

            mwApi.appendEdit(editToken,userPageString+"\n",
                    "User_Talk:"+sessionManager.getCurrentAccount().name, summary);
            publishProgress(5);
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

        int[] messages = new int[]{
                R.string.getting_edit_token,
                R.string.nominate_for_deletion_edit_file_page,
                R.string.nominate_for_deletion_create_deletion_request,
                R.string.nominate_for_deletion_edit_deletion_request_log,
                R.string.nominate_for_deletion_notify_user,
                R.string.nominate_for_deletion_done
        };

        String message = "";
        if (0 < values[0] && values[0] < messages.length) {
            message = context.getString(messages[values[0]]);
        }

        notificationBuilder.setContentTitle(context.getString(R.string.nominating_file_for_deletion, media.getFilename()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(5, values[0], false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_DELETE, notificationBuilder.build());
    }

    @Override
    protected void onPostExecute(Boolean result) {
        String message = "";
        String title = "Nominating for Deletion";

        if (result){
            title += ": Success";
            message = "Successfully nominated " + media.getDisplayTitle() + " deletion.";
        }
        else {
            title += ": Failed";
            message = "Could not request deletion.";
        }

        notificationBuilder.setDefaults(DEFAULT_ALL)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(0,0,false)
                .setOngoing(false)
                .setPriority(PRIORITY_HIGH);
        notificationManager.notify(NOTIFICATION_DELETE, notificationBuilder.build());
    }
}