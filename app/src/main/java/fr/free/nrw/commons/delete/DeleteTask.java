package fr.free.nrw.commons.delete;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.review.ReviewActivity;
import timber.log.Timber;

import static androidx.core.app.NotificationCompat.DEFAULT_ALL;
import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

public class DeleteTask extends AsyncTask<Void, Integer, Boolean> {

    @Inject MediaWikiApi mwApi;
    @Inject SessionManager sessionManager;

    private static final int NOTIFICATION_DELETE = 1;

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
    protected void onPreExecute() {
        ApplicationlessInjection
                .getInstance(context.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(
                context,
                CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)
                .setOnlyAlertOnce(true);
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
                    "User_Talk:"+ sessionManager.getCurrentAccount().name,summary);
            publishProgress(5);
        }
        catch (Exception e) {
            Timber.e(e);
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
        String message;
        String title = "Nominating for Deletion";

        if (result){
            title += ": Success";
            message = "Successfully nominated " + media.getDisplayTitle() + " for deletion.";
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
        String urlForDelete = BuildConfig.COMMONS_URL + "/wiki/Commons:Deletion_requests/File:" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW , Uri.parse(urlForDelete));
        PendingIntent pendingIntent = PendingIntent.getActivity(context , 1 , browserIntent , PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationManager.notify(NOTIFICATION_DELETE, notificationBuilder.build());
    }

    // TODO: refactor; see MediaDetailsFragment.onDeleteButtonClicked
    // ReviewActivity will use this
    public static void askReasonAndExecute(Media media, Context context, String question, String problem) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(question);

        boolean[] checkedItems = {false , false, false, false};
        ArrayList<Integer> mUserReason = new ArrayList<>();

        String[] reasonList= {"Reason 1","Reason 2","Reason 3","Reason 4"};


        if(problem.equals("spam")){
            reasonList[0] = "A selfie";
            reasonList[1] = "Blurry";
            reasonList[2] = "Nonsense";
            reasonList[3] = "Other";
        }
        else if(problem.equals("copyRightViolation")){
            reasonList[0] = "Press photo";
            reasonList[1] = "Random photo from internet";
            reasonList[2] = "Logo";
            reasonList[3] = "Other";
        }

        alert.setMultiChoiceItems(reasonList, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                if(isChecked){
                    mUserReason.add(position);
                }else{
                    mUserReason.remove((Integer.valueOf(position)));
                }
            }
        });

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String reason = "Because it is ";
                for (int j = 0; j < mUserReason.size(); j++) {
                    reason = reason + reasonList[mUserReason.get(j)];
                    if (j != mUserReason.size() - 1) {
                        reason = reason + ", ";
                    }
                }

                ((ReviewActivity)context).reviewController.swipeToNext();
                ((ReviewActivity)context).runRandomizer();

                DeleteTask deleteTask = new DeleteTask(context, media, reason);
                deleteTask.execute();
            }
        });
        alert.setNegativeButton("Cancel" , null);


        AlertDialog d = alert.create();
        d.show();

    }
}
