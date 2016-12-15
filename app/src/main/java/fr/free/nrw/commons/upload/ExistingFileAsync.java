package fr.free.nrw.commons.upload;


import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import org.mediawiki.api.ApiResult;
import org.mediawiki.api.MWApi;

import java.io.IOException;
import java.util.ArrayList;

import android.support.v7.app.AlertDialog;

import fr.free.nrw.commons.CommonsApplication;

/**
 * Sends asynchronous queries to the Commons MediaWiki API to check that file doesn't already exist
 * Returns true if file exists, false if it doesn't
 */
public class ExistingFileAsync extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = fr.free.nrw.commons.upload.ExistingFileAsync.class.getName();

    private String fileSHA1;
    private Context context;

    private AlertDialog alertDialog;

    public ExistingFileAsync(String fileSHA1, Context context) {
        super();
        this.fileSHA1 = fileSHA1;
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        alertDialog = new AlertDialog.Builder(ExistingFileAsync.this);
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
            Log.d(TAG, "Searching Commons API for existing file: " + result.toString());
        } catch (IOException e) {
            Log.e(TAG, "IO Exception: ", e);
            return false;
        }

        ArrayList<ApiResult> resultNodes = result.getNodes("/api/query/allimages/img");
        Log.d(TAG, "Result nodes: " + resultNodes);

        boolean fileExists;
        if (!resultNodes.isEmpty()) {
            fileExists = true;
        } else {
            fileExists = false;
        }

        Log.d(TAG, "File already exists in Commons:" + fileExists);
        return fileExists;
    }

    @Override
    protected void onPostExecute(Boolean fileExists) {
        super.onPostExecute(fileExists);
        //TODO: Add Dialog here to tell user file exists, do you want to continue? Yes/No

        alertDialog.setTitle("The Process");
        alertDialog.setIcon(R.drawable.success);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setMessage("All done!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent A = new Intent(DownloadActivity.this, Menu_activity.class);
                        startActivity(A);
                        finish();
                    }
                });
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Intent A = new Intent(DownloadActivity.this, Menu_activity.class);
                startActivity(A);
                finish();
            }
        });
        alertDialog.show();
    }
}