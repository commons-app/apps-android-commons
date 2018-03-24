package fr.free.nrw.commons.delete;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
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

public class DeleteTask extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS = 0;
    private static final int FAILED = -1;
    private static final int ALREADY_DELETED = -2;

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
    protected Integer doInBackground(Void ...voids) {
        String editToken;
        String authCookie;
        String summary = "Nominating " + media.getFilename() +" for deletion.";

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        try{
            if (mwApi.pageExists("Commons:Deletion_requests/"+media.getFilename())){
                return ALREADY_DELETED;
            }
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return FAILED;
        }

        try {
            editToken = mwApi.getEditToken();
        }
        catch (Exception e){
            Timber.d(e.getMessage());
            return FAILED;
        }
        if (editToken.equals("+\\")) {
            return FAILED;
        }

        Calendar calendar = Calendar.getInstance();
        String fileDeleteString = "{{delete|reason=" + reason +
                "|subpage=" +media.getFilename() +
                "|day=" + calendar.get(Calendar.DAY_OF_MONTH) +
                "|month=" + calendar.getDisplayName(Calendar.MONTH,Calendar.LONG, Locale.getDefault()) +
                "|year=" + calendar.get(Calendar.YEAR) +
                "}}";
        try{
            mwApi.prependEdit(editToken,fileDeleteString+"\n",
                    media.getFilename(),summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return FAILED;
        }

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";
        try{
            mwApi.edit(editToken,subpageString+"\n",
                    "Commons:Deletion_requests/"+media.getFilename(),summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return FAILED;
        }

        String logPageString = "\n{{Commons:Deletion requests/" + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String date = sdf.format(calendar.getTime());
        try{
            mwApi.appendEdit(editToken,logPageString+"\n",
                    "Commons:Deletion_requests/"+date,summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return FAILED;
        }

        String userPageString = "\n{{subst:idw|" + media.getFilename() +
                "}} ~~~~";
        try{
            mwApi.appendEdit(editToken,userPageString+"\n",
                    "User_Talk:"+sessionManager.getCurrentAccount().name,summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return FAILED;
        }
        return SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer result) {
        String message = "";
        String title = "";
        switch (result){
            case SUCCESS:
                title = "Success";
                message = "Successfully nominated " + media.getDisplayTitle() + " deletion.\n" +
                        "Check the webpage for more details";
                break;
            case FAILED:
                title = "Failed";
                message = "Could not request deletion. Something went wrong.";
                break;
            case ALREADY_DELETED:
                title = "Already Nominated";
                message = media.getDisplayTitle() + " has already been nominated for deletion.\n" +
                        "Check the webpage for more details";
                break;
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
                        startActivity(context,browserIntent,null);
                    }
                });
        alert = builder.create();
        alert.show();
    }
}