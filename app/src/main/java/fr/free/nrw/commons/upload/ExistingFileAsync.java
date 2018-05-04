package fr.free.nrw.commons.upload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;

import java.io.IOException;
import java.lang.ref.WeakReference;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
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

    private final WeakReference<Activity> activity;
    private final MediaWikiApi api;
    private final String fileSha1;
    private final WeakReference<Context> context;
    private final Callback callback;

    public ExistingFileAsync(WeakReference<Activity> activity, String fileSha1, WeakReference<Context> context, Callback callback, MediaWikiApi mwApi) {
        this.activity = activity;
        this.fileSha1 = fileSha1;
        this.context = context;
        this.callback = callback;
        this.api = mwApi;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        // https://commons.wikimedia.org/w/api.php?action=query&list=allimages&format=xml&aisha1=801957214aba50cb63bb6eb1b0effa50188900ba
        boolean fileExists;
        try {
            String fileSha1 = this.fileSha1;
            fileExists = api.existingFile(fileSha1);
        } catch (IOException e) {
            Timber.e(e, "IO Exception: ");
            return false;
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
            AlertDialog.Builder builder = new AlertDialog.Builder(context.get());
            builder.setMessage(R.string.file_exists)
                    .setTitle(R.string.warning);
            builder.setPositiveButton(R.string.no, (dialog, id) -> {
                //Go back to ContributionsActivity
                Intent intent = new Intent(context.get(), ContributionsActivity.class);
                context.get().startActivity(intent);
                callback.onResult(Result.DUPLICATE_CANCELLED);
            });
            builder.setNegativeButton(R.string.yes, (dialog, id) -> callback.onResult(Result.DUPLICATE_PROCEED));

            AlertDialog dialog = builder.create();
            if (!activity.get().isFinishing()) {
                dialog.show();
            }
        } else {
            callback.onResult(Result.NO_DUPLICATE);
        }
    }
}