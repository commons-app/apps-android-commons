package fr.free.nrw.commons.delete;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static android.view.View.GONE;

public class DeleteTask extends AsyncTask<Void, Void, Boolean> {

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

        try {
            editToken = mwApi.getEditToken();
        }
        catch (Exception e){
            Timber.d(e.getMessage());
            return false;
        }
        if (editToken.equals("+\\")) {
            return false;
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
            return false;
        }

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";
        try{
            mwApi.edit(editToken,subpageString+"\n",
                    "Commons:Deletion requests/"+media.getFilename(),summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return false;
        }

        String logPageString = "\n{{Commons:Deletion requests/" + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String date = sdf.format(calendar.getTime());
        try{
            mwApi.appendEdit(editToken,logPageString+"\n",
                    "Commons:Deletion requests/"+date,summary);
        }
        catch (Exception e) {
            Timber.d(e.getMessage());
            return false;
        }

        String userPageString = "\n{{subst:idw|" + media.getFilename() +
                "}} ~~~~";
        try{
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
    protected void onPostExecute(Boolean result) {
        String toastText = "";
        if (result) {
            toastText = "Successfully requested deletion.";
        }
        else{
            toastText = "Could not request deletion.";
        }
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }
}