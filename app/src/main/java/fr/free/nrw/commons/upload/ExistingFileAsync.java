package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.util.ArrayList;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.MWApi;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.libs.mediawiki_api.ApiResult;
import timber.log.Timber;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to check that file doesn't already exist
 * Displays a warning to the user if the file already exists on Commons
 */
public class ExistingFileAsync extends AsyncTask<Void, Void, Boolean> {
    interface Callback {
        void onResult(Result result);
    }

    public enum Result {
        NO_DUPLICATE,
        DUPLICATE_PROCEED,
        DUPLICATE_CANCELLED
    }

    private final String fileSha1;
    private final Context context;
    private final Callback callback;

    public ExistingFileAsync(String fileSha1, Context context, Callback callback) {
        this.fileSha1 = fileSha1;
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        MWApi api = CommonsApplication.getInstance().getMWApi();
        ApiResult result;

        // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
        try {
            result = api.action("query")
                    .param("format", "xml")
                    .param("list", "allimages")
                    .param("aisha1", fileSha1)
                    .prepareHttpRequestBuilder("GET")
                    .request();;
            Timber.d("Searching Commons API for existing file: %s", result);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            return false;
        }

        ArrayList<ApiResult> resultNodes = result.getNodes("/api/query/allimages/img");
        Timber.d("Result nodes: %s", resultNodes);

        boolean fileExists = !resultNodes.isEmpty();

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
                    callback.onResult(Result.DUPLICATE_CANCELLED);
                }
            });
            builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    callback.onResult(Result.DUPLICATE_PROCEED);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            callback.onResult(Result.NO_DUPLICATE);
        }
    }
}