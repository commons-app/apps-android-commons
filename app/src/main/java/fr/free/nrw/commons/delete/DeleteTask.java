package fr.free.nrw.commons.delete;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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

import static android.support.v4.content.ContextCompat.startActivity;
import static android.widget.Toast.LENGTH_SHORT;

public class DeleteTask extends AsyncTask<Void, Integer, Boolean> {

    @Inject MediaWikiApi mwApi;
    @Inject SessionManager sessionManager;

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
    }

    @Override
    protected Boolean doInBackground(Void ...voids) {
        String editToken;
        String authCookie;
        String summary = "Nominating " + media.getFilename() +" for deletion.";

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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
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
                    media.getFilename(),summary);
            publishProgress(2);

            mwApi.edit(editToken,subpageString+"\n",
                    "Commons:Deletion_requests/"+media.getFilename(),summary);
            publishProgress(3);

            mwApi.appendEdit(editToken,logPageString+"\n",
                    "Commons:Deletion_requests/"+date,summary);
            publishProgress(4);

            mwApi.appendEdit(editToken,userPageString+"\n",
                    "User_Talk:"+sessionManager.getCurrentAccount().name,summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onProgressUpdate (Integer... values){

    }

    @Override
    protected void onPostExecute(Boolean result) {
        String message = "";
        String title = "";

        if (result){
            title = "Success";
            message = "Successfully nominated " + media.getDisplayTitle() + " deletion.\n" +
                    "Check the webpage for more details";
        }
        else {
            title = "Failed";
            message = "Could not request deletion. Something went wrong.";
        }

        AlertDialog alert;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(
                R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {}
                });
        builder.setNeutralButton(R.string.view_browser,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, media.getFilePageTitle().getMobileUri());
                        if (browserIntent.resolveActivity(context.getPackageManager()) != null) {
                            startActivity(context,browserIntent,null);
                        } else {
                            Toast toast = Toast.makeText(context, R.string.no_web_browser, LENGTH_SHORT);
                            toast.show();
                        }
                    }
                });
        alert = builder.create();
        alert.show();
    }
}