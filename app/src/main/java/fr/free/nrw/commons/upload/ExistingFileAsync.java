package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import timber.log.Timber;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to check that file doesn't already exist
 * Displays a warning to the user if the file already exists on Commons
 */
public class ExistingFileAsync extends AsyncTask<Void, Void, Boolean> {

    private String fileSHA1;
    private Context context;

    public ExistingFileAsync(String fileSHA1, Context context) {
        this.fileSHA1 = fileSHA1;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        MWApi api = CommonsApplication.createMWApi();
        ApiResult result;

        // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
        try {
            result = api.action("query")
                    .param("format", "xml")
                    .param("list", "allimages")
                    .param("aisha1", fileSHA1)
                    .get();
            Timber.d("Searching Commons API for existing file: %s", result);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            return false;
        }

        ArrayList<ApiResult> resultNodes = result.getNodes("/api/query/allimages/img");
        Timber.d("Result nodes: %s", resultNodes);

        boolean fileExists;
        if (!resultNodes.isEmpty()) {
            fileExists = true;
        } else {
            fileExists = false;
        }

        Timber.d("File already exists in Commons: %s", fileExists);
        return fileExists;
    }

    @Override
    protected void onPostExecute(Boolean fileExists) {
        super.onPostExecute(fileExists);

        // If file exists, display warning to user.
        // Use soft warning for now (user able to choose to proceed) until have determined that implementation works without bugs
        if (fileExists) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.file_exists)
                    .setTitle(R.string.warning);
            builder.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    //Go back to ContributionsActivity
                    Intent intent = new Intent(context, ContributionsActivity.class);
                    context.startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    //No need to do anything, user remains on upload screen
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
}